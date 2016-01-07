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

import java.util.Properties

import org.apache.zeppelin.interpreter.InterpreterResult.Code
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks

@RunWith(classOf[JUnitRunner])
class EmmaReplSpec extends FlatSpec with Matchers with PropertyChecks {
  import EmmaRepl._

  implicit override val generatorDrivenConfig =
    PropertyCheckConfig(minSuccessful = 10, workers = 1)

  "the Emma REPL" should "detect incomplete code" in {
    withNative { repl =>
      val result = repl interpret "val x = "
      result.code should be (Code.INCOMPLETE)
    }
  }

  it should "detect compilation errors" in {
    withNative { repl =>
      val result = repl interpret "val x: Int = \"42\""
      result.code    should be      (Code.ERROR)
      result.message should include ("error: type mismatch")
    }
  }

  it should "handle correct code without errors" in {
    withNative { repl =>
      val result = repl interpret "val x = 42"
      result.code         should be (Code.SUCCESS)
      result.message.trim should be ("x: Int = 42")
    }
  }

  it should "have autocompletion" in {
    withNative { repl =>
      val candidates = repl.complete("val x: Str", 10)
      candidates       should not be empty
      all (candidates) should startWith ("Str")
    }
  }

  it should "have a predefined native runtime" in {
    withNative { repl =>
      val result = repl interpret "engine"
      result.code    should be      (Code.SUCCESS)
      result.message should include ("Native")
    }
  }

  it should "have a predefined Flink runtime" in {
    withFlink { repl =>
      val result = repl interpret "engine"
      result.code    should be      (Code.SUCCESS)
      result.message should include ("Flink")
    }
  }

  it should "have a predefined Spark runtime" in {
    withSpark { repl =>
      val result = repl interpret "engine"
      result.code    should be      (Code.SUCCESS)
      result.message should include ("Spark")
    }
  }

  it should "run a simple sum algorithm" in {
    forAll { xs: Seq[Int] =>
      withNative { repl =>
        val result = repl.interpret(s"""
          emma.parallelize {
            DataBag($xs: Seq[Int]).sum()
          }.run(engine)""")

        println(result.message)
        result.code    should be      (Code.SUCCESS)
        result.message should include (xs.sum.toString)
      }
    }
  }

  def withRepl(options: (String, String)*)(f: EmmaRepl => Unit) = {
    val props = new Properties()
    for ((k, v) <- options) props.setProperty(k, v)
    val repl  = new EmmaRepl(props)
    repl.init()
    f(repl)
    repl.close()
  }

  def withNative(f: EmmaRepl => Unit) =
    withRepl(`x.backend` -> "native")(f)
  
  def withFlink(f: EmmaRepl => Unit) =
    withRepl(`x.backend` -> "flink", `flink.path` -> lib("flink"))(f)

  def withSpark(f: EmmaRepl => Unit) =
    withRepl(`x.backend` -> "spark", `spark.path` -> lib("spark"))(f)
  
  def lib(runtime: String) =
    s"${System.getProperty("project.base.dir", "..")}/../$runtime/target/pack/lib"
}
