/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

val versions = new {
  val emma       = "1.0-SNAPSHOT"
  val flink      = "0.10.0"
  val spark      = "1.3.1"
  val hadoop     = "2.2.0"
  val zeppelin   = "0.6.0-incubating-SNAPSHOT"
  val junit      = "4.12"
  val scalatest  = "2.2.4"
  val scalacheck = "1.12.4"
}

val deploy = inputKey[Unit]("Deploy to a Zeppelin installation")

val deployTarget = SettingKey[File]("Deployment path to a Zeppelin installation")

val deploySubdir = SettingKey[String]("Deployment subdirectory for current module")

lazy val common = packSettings ++ Seq( // common settings
  organization := "eu.stratosphere.emma",
  version      := "1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  fork         := true,
  // enforce Java version
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
  // Maven local repository
  resolvers += "Maven Local" at s"file://${Path.userHome.absolutePath}/.m2/repository",
  // set PATH for testing
  javaOptions in Test += s"-Dproject.base.dir=${baseDirectory.value.absolutePath}",
  // default deploy target
  deployTarget <<= target,
  deploySubdir  := "interpreter/emma",
  // define deployment task
  deploy := Def.inputTask {
    val log  = sLog.value
    val args = Def.spaceDelimited("<target directory>").parsed
    val path = if (args.isEmpty) {
      sys.env get "ZEPPELIN_HOME" match {
        case Some(home) => home
        case None =>
          log warn "Target directory or ZEPPELIN_HOME environment variable not specified"
          deployTarget.value.absolutePath
      }
    } else args.head
    val source = (pack.value / "lib").get.head
    val target = file(s"$path/${deploySubdir.value}")
    log info s"Deploying from ${source.absolutePath} to ${target.absolutePath}"
    IO.copyDirectory(source, target, overwrite = true, preserveLastModified = true)
  }.evaluated)

lazy val native = project.in(file("native")).settings(common: _*).settings(
  name        := "zeppelin-emma-native",
  description := "Zeppelin interpreter for the Emma language",
  packArchiveName <<= name,

  projectDependencies ++= Seq( // Scala
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "org.scala-lang" % "scala-library"  % scalaVersion.value,
    "org.scala-lang" % "scala-reflect"  % scalaVersion.value),

  projectDependencies ++= Seq( // Zeppelin
    "org.apache.zeppelin" % "zeppelin-interpreter" % versions.zeppelin
  ) map { _ % "provided" },

  projectDependencies ++= Seq( // Emma
    "eu.stratosphere" % "emma-common"        % versions.emma,
    "eu.stratosphere" % "emma-common-macros" % versions.emma,
    "eu.stratosphere" % "emma-language"      % versions.emma
  ) map { _ % "runtime" },

  projectDependencies ++= Seq( // HDFS
    "org.apache.hadoop" % "hadoop-common" % versions.hadoop,
    "org.apache.hadoop" % "hadoop-hdfs"   % versions.hadoop
  ) map { _ % "runtime" },

  libraryDependencies ++= Seq( // Testing
    "junit"          %  "junit"      % versions.junit,
    "org.scalatest"  %% "scalatest"  % versions.scalatest,
    "org.scalacheck" %% "scalacheck" % versions.scalacheck
  ) map { _ % "test" })

lazy val flink = project.in(file("flink")).settings(common: _*).settings(
  name        := "zeppelin-emma-flink",
  description := "Zeppelin interpreter for Emma on Flink",
  packArchiveName <<= name,
  deploySubdir     += "/flink",

  libraryDependencies ++= Seq( // Flink
    "eu.stratosphere"  %  "emma-flink"    % versions.emma,
    "org.apache.flink" %% "flink-clients" % versions.flink,
    "org.apache.flink" %% "flink-java"    % versions.flink,
    "org.apache.flink" %% "flink-scala"   % versions.flink
  ) map { _ % "runtime" })

lazy val spark = project.in(file("spark")).settings(common: _*).settings(
  name        := "zeppelin-emma-spark",
  description := "Zeppelin interpreter for Emma on Spark",
  packArchiveName <<= name,
  deploySubdir     += "/spark",

  libraryDependencies ++= Seq( // Spark
    "eu.stratosphere"  %  "emma-spark" % versions.emma,
    "org.apache.spark" %% "spark-core" % versions.spark
  ) map { _ % "runtime" })
