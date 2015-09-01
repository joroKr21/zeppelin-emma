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
package eu.stratosphere.emma.zeppelin;

import java.nio.file.Paths;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.zeppelin.interpreter.*;

/**
 * An interactive Zeppelin interpreter for the Emma language.
 */
public class EmmaInterpreter extends Interpreter {

  private static final Logger log = Logger.getLogger(EmmaInterpreter.class);

  static {
    Map<String, InterpreterProperty> settings = new InterpreterPropertyBuilder()
      .add("emma.codegen.dir",
           Paths.get(System.getProperty("java.io.tmpdir"), "emma", "codegen").toString(),
           "Temporary directory for Emma-generated classes")
      .add("emma.execution.backend", "native",    "Runtime backend {[native],flink,spark}")
      .add("emma.execution.mode",    "local",     "Execution mode {[local],remote}")
      .add("emma.execution.host",    "localhost", "Master node address (when mode=remote)")
      .add("emma.execution.port",    "6123",      "Runtime backend port number")
      .add("emma.flink.path",
           Paths.get(".", "interpreter", "emma", "flink").toAbsolutePath().toString(),
           "Path to the emma-flink library (jars)")
      .add("emma.spark.path",
           Paths.get(".", "interpreter", "emma", "spark").toAbsolutePath().toString(),
           "Path to the emma-spark library (jars)")
      .build();

    String className = EmmaInterpreter.class.getName();
    Interpreter.register("emma", "emma", className, settings);
  }

  private EmmaRepl repl;

  public EmmaInterpreter(Properties properties) {
    super(properties);
    repl = new EmmaRepl(properties);
  }

  @Override public void open() {
    repl.init();
  }

  @Override public void close() {
    repl.close();
  }

  @Override public InterpreterResult interpret(String str, InterpreterContext context) {
    try {
      return repl.interpret(str);
    } catch (Exception e) {
      String message = InterpreterUtils.getMostRelevantMessage(e);
      return new InterpreterResult(InterpreterResult.Code.ERROR, message);
    }
  }

  @Override public void cancel(InterpreterContext context) { }

  @Override public FormType getFormType() {
    return FormType.NATIVE;
  }

  @Override public int getProgress(InterpreterContext context) {
    return 0;
  }

  @Override public List<String> completion(String buffer, int cursor) {
    return repl.complete(buffer, cursor);
  }
}
