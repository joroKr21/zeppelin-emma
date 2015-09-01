package eu.stratosphere.emma

import scala.language.dynamics

package object zeppelin {

  implicit class PrivateCaller[A](val self: A) extends AnyVal {
    def p: Private[A] = new Private(self)
  }
}
