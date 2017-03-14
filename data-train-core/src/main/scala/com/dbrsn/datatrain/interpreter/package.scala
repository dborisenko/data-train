package com.dbrsn.datatrain

package object interpreter {
  type ErrorOr[A] = Either[Throwable, A]
}
