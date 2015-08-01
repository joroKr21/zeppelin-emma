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

`TODO`

Now, assuming you have installed all dependencies:
* To run all tests (and check that all is fine) use `./sbt test`
    * Beware that the first time it will take long to download `sbt`
* To deploy to an existing Zeppelin installation use one of:
    * `./sbt deploy [Zeppelin home]`
    * `ZEPPELIN_HOME=[Zeppelin home] ./sbt deploy`
    
