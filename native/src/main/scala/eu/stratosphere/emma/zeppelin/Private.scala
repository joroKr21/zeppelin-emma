package eu.stratosphere.emma.zeppelin

import scala.language.dynamics

class Private[A](self: A) extends Dynamic {
  lazy val parents: Stream[Class[_]] =
    self.getClass #:: parents.map { _.getSuperclass }

  def applyDynamic(method: String)(args: Any*): Any = {
    val _args    = args map { _.asInstanceOf[AnyRef] }
    val _methods = parents takeWhile { _ != null } flatMap { _.getDeclaredMethods }
    val _method  = _methods
      .find { _.getName == method }
      .getOrElse(throw new NoSuchMethodException(s"method $method not found"))

    _method.setAccessible(true)
    _method.invoke(self, _args : _*)
  }

  def selectDynamic(field: String): Any =
    applyDynamic(field)()

  def updateDynamic(field: String)(value: Any): Unit =
    applyDynamic(s"${field}_$$eq")(value)
}
