# zeppelin-emma
Welcome to the zeppelin-emma project, an interactive
[Apache Zeppelin](https://zeppelin.incubator.apache.org/) interpreter for the
[Emma](http://emma-language.org/) language.

### Testing, Running and Deployment
This project is managed via [`sbt`](http://www.scala-sbt.org/), but you don't necessarily have to
install it on your system, because we ship a scripted version under `./sbt`. However, you do need
[`git`](https://git-scm.com/) (duh) and [`mvn`](https://maven.apache.org/) (for Zeppelin and Emma).

Since most project that this interpreter depends on don't provide binary releases yet (including
Zeppelin and Emma), we have to build them from source, which is a somewhat lenghty process, but
needs to be done only once. Here is a sequence of steps you can follow that lead to a running
Zeppelin installation with the Emma interpreter included (i.e. on a \*nix system):

1. Clone [Zeppelin](https://github.com/apache/incubator-zeppelin) and install it locally.
(Optionally set the `ZEPPELIN_HOME` environment variable to the project root):
    
    ```
    git clone https://github.com/apache/incubator-zeppelin.git
    cd incubator-zeppelin
    mvn install -DskipTests
    ```
    
2. Clone [Emma](https://github.com/stratosphere/emma) and install it locally:

    ```
    git clone https://github.com/stratosphere/emma.git
    cd emma
    mvn install -DskipTests
    ```
    
3. Clone [Zeppelin-Emma](https://github.com/joroKr21/zeppelin-emma):

    ```
    git clone https://github.com/joroKr21/zeppelin-emma.git
    ```
    
    You are now ready to deploy the interpreter to Zeppelin.
    
4. Now, assuming you have installed all dependencies:
    * To run all tests (and check that all is fine) use `./sbt test`
        * Beware that the first time it will take long to download `sbt`
    * To deploy to an existing Zeppelin installation use one of
    (Zeppelin home should point to the root of your local Zeppelin repo):
        * `./sbt deploy [Zeppelin home]`
        * `ZEPPELIN_HOME=[Zeppelin home] ./sbt deploy`
    * To run Zeppelin with Emma enabled:
        1. In Zeppelin home, edit the configuration:
            
            ```
            cp conf/zeppelin-site.xml.template conf/zeppelin-site.xml
            vim conf/zeppelin-site.xml
            ```
            
            Then add the following entry to the `zeppelin.interpreter` list:
            
            `eu.stratosphere.emma.zeppelin.EmmaInterpreter`
            
        2. Run Zeppelin as a script or daemon:
            * `bin/zeppelin.sh`
            * `bin/zeppelin-daemon.sh start` (stop it with `bin/zeppelin-daemon.sh stop`)
