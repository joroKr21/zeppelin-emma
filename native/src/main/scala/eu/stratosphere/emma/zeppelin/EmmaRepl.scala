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
package eu.stratosphere.emma.zeppelin

import java.io._
import java.net.URL
import java.util.{Map => JMap}
import java.util.Properties

import org.apache.zeppelin.interpreter.InterpreterResult

import scala.collection.JavaConversions._
import scala.Console.{withErr, withOut}
import scala.language.implicitConversions
import scala.tools.jline_embedded.console.completer._
import scala.tools.nsc._
import scala.tools.nsc.interpreter._
import scala.tools.nsc.interpreter.Completion._

class EmmaRepl(properties: Properties) {
  import EmmaRepl._

  private val options = defaults ++ properties
  private val buffer  = new ByteArrayOutputStream
  private val writer  = new PrintWriter(buffer)
  private var jLine: ArgumentCompleter = _

  private val scalaRepl = {
    val settings = new Settings(writer.println)
    settings.embeddedDefaults(Thread.currentThread.getContextClassLoader)
    settings.usejavacp.value = true

    for (dir <- options get `codegen.dir`)
      settings.classpath append dir

    options get `x.backend` match {
      case Some("flink") =>
        for (path <- options get `flink.path`)
          settings.classpath append path.\*

      case Some("spark") =>
        for (path <- options get `spark.path`)
          settings.classpath append path.\*

      case _ =>
    }

    new IMain(settings, writer)
  }

  def init() {
    scalaRepl.initializeSynchronous()
    scalaRepl.ensureClassLoader()
    val completion = new JLineCompletion(scalaRepl)
    jLine = new ArgumentCompleter(scalaToJline(completion.completer()))
    jLine setStrict false

    val runtime = options get `x.backend` match {
      case Some(backend) => s"${backend.capitalize}()"
      case None => "Native()"
    }

    val cp = scalaRepl.compilerClasspath map { _.getFile } mkString ":"

    scalaRepl beQuietDuring {
      for ((key, value) <- options) interpret(s"""sys.props += "$key" -> "$value"""")
      interpret(s"""sys.props += "${`runtime.cp`}" -> "$cp"""")
      interpret("import eu.stratosphere.emma.api._")
      interpret(s"val engine = new eu.stratosphere.emma.runtime.$runtime")
      interpret(s"val native = eu.stratosphere.emma.runtime.Native()")
    }
  }

  def close(): Unit = {
    interpret("engine.closeSession()")
    interpret("native.closeSession()")
    scalaRepl.close()
    writer.close()
    buffer.close()
  }

  def complete(buffer: String, cursor: Int): JList[String] = {
    val candidates = new java.util.ArrayList[CharSequence]
    jLine.complete(buffer, cursor, candidates)
    candidates map { _.toString }
  }

  def interpret(str: String): InterpreterResult =
    withOut(buffer) {
      withErr(buffer) {
        val code = scalaRepl interpret s"$str\n"
        writer.flush()
        val message = buffer.toString.trim.comprehend
        buffer.reset()
        new InterpreterResult(code, message)
      }
    }

  private def jarsIn(dir: String) = for {
    file <- new File(dir).listFiles
    path  = file.getAbsolutePath
    if path endsWith ".jar"
  } yield new URL(s"file://$path")

  private def scalaToJline(sc: ScalaCompleter): Completer = new Completer {
    def complete(buffer: String, cursor: Int, candidates: JList[CharSequence]) = {
      val str = if (buffer == null) "" else buffer
      val Candidates(cur, cs) = sc.complete(str, cursor)
      candidates addAll cs
      cur
    }
  }

  private implicit def irToCode(ir: IR.Result): InterpreterResult.Code = {
    import InterpreterResult.Code

    ir match {
      case IR.Success    => Code.SUCCESS
      case IR.Incomplete => Code.INCOMPLETE
      case IR.Error      => Code.ERROR
    }
  }
}

object EmmaRepl {
  val `codegen.dir` = "emma.codegen.dir"
  val `x.backend`   = "emma.execution.backend"
  val `x.mode`      = "emma.execution.mode"
  val `x.host`      = "emma.execution.host"
  val `x.port`      = "emma.execution.port"
  val `flink.path`  = "emma.flink.path"
  val `spark.path`  = "emma.spark.path"
  val `runtime.cp`  = "emma.runtime.classpath"

  val defaults: JMap[String, String] = Map(
    `codegen.dir` -> s"${sys props "java.io.tmpdir"}/emma/codegen",
    `x.backend`   -> "native",
    `x.mode`      -> "local",
    `x.host`      -> "localhost",
    `x.port`      -> "6123",
    `flink.path`  -> "./interpreter/emma/flink",
    `spark.path`  -> "./interpreter/emma/spark")

  val descriptions: JMap[String, String] = Map(
    `codegen.dir` -> "Temporary directory for Emma-generated classes",
    `x.backend`   -> "Runtime backend {[native],flink,spark}",
    `x.mode`      -> "Execution mode {[local],remote}",
    `x.host`      -> "Master node address (when mode=remote)",
    `x.port`      -> "Runtime backend port number",
    `flink.path`  -> "Path to the emma-flink library (jars)",
    `spark.path`  -> "Path to the emma-spark library (jars)")

  implicit class Wildcard(val self: String) extends AnyVal {
    def \* = self.reverse.dropWhile { _ == '/' }.reverse + "/*"
  }

  implicit class Comprehend(val self: String) extends AnyVal {
    def isComprehension: Boolean =
      self.takeWhile { _ == '~' }.length > 10

    def comprehend: String = if (isComprehension)
      self.split("~+").map { _.trim }.filter { _.nonEmpty }.sliding(2, 2).map {
        case Array(head, code) =>
          <div>
            <h4>{head}</h4>
            <pre><code class="hljs scala">{code}</code></pre>
          </div>
        case _ => ""
      }.mkString("%html ", "\n", "")
    else self
  }
}
