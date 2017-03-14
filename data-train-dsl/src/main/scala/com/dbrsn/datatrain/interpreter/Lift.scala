package com.dbrsn.datatrain.interpreter

import cats.data.Kleisli
import cats.~>

import scala.language.higherKinds

object Lift {
  def toKleisli[F[_], R] = new (F ~> Kleisli[F, R, ?]) {
    override def apply[A](fa: F[A]): Kleisli[F, R, A] = Kleisli[F, R, A](_ => fa)
  }
}
