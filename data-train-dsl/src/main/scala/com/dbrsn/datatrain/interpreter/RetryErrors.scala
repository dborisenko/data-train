package com.dbrsn.datatrain.interpreter

import cats.MonadError
import cats.~>

import scala.language.higherKinds

class RetryErrors[F[_], M[_], E](interpreter: F ~> M, retries: E => Int)(implicit ME: MonadError[M, E]) extends (F ~> M) {
  override def apply[A](fa: F[A]): M[A] = {
    def attempt(attempts: Int): M[A] = ME.handleErrorWith(interpreter(fa)) { e =>
      if (attempts < retries(e))
        attempt(attempts + 1)
      else
        ME.raiseError(e)
    }
    attempt(0)
  }
}
