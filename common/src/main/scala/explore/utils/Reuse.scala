// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.utils

import japgolly.scalajs.react.Reusability

import scala.reflect.runtime.universe._

trait ReuseSyntax {
  implicit class AnyReuseOps[A]( /*val*/ a: A) { //extends AnyVal {
    def always: Reuse[A] = ???
    def never: Reuse[A]  = ???
    def curry[R, S, B](
      r:        R
    )(implicit
      ev:       A =:= ((R, S) => B),
      typeTagR: TypeTag[R],
      reuseR:   Reusability[R]
    ): Reuse[S => B] = ???
  }
}

object ReuseSyntax extends ReuseSyntax

/*
 * This is a fledgling idea for a mechanism to provide Reusability for functions.
 * It's not used yet, will develop in future PR.
 */
trait Reuse[A] {
  type B

  val value: () => A

  protected val reuseBy: B // Do we need to store this if reusability function has it already?

  protected val typeTag: TypeTag[B]

  protected val reusability: Reusability[B] // Turn into "B => Boolean"

  def andBy[C](c: C)(implicit typeTagC: TypeTag[C], reuseC: Reusability[C]): Reuse[A] = ???

  def orBy[C](c: C)(implicit typeTagC: TypeTag[C], reuseC: Reusability[C]): Reuse[A] = ???
}

object Reuse {
  implicit def toA[A](reuseFn: Reuse[A]): A = reuseFn.value()

  implicit def reusability[A]: Reusability[Reuse[A]] =
    Reusability.apply((reuseA, reuseB) =>
      if (reuseA.typeTag == reuseB.typeTag)
        reuseA.reusability.test(reuseA.reuseBy, reuseB.reuseBy.asInstanceOf[reuseA.B])
      else false
    )

  def by[A, R](
    reuseByR: R
  )(valueA:   => A)(implicit typeTagR: TypeTag[R], reuseR: Reusability[R]): Reuse[A] =
    new Reuse[A] {
      type B = R

      val value = () => valueA

      protected val reuseBy = reuseByR

      protected val typeTag = typeTagR

      protected val reusability = reuseR
    }

  def always[A](a: A): Reuse[A] = ???

  def never[A](a: A): Reuse[A] = ???

  def apply[A](value: => A): Applied[A] = new Applied(value)

  class Applied[A](valueA: => A) {
    val value: A = valueA // TODO Propagate laziness

    def by[R](reuseByR: R)(implicit typeTagR: TypeTag[R], reuseR: Reusability[R]): Reuse[A] =
      Reuse.by(reuseByR)(valueA)

    def always: Reuse[A] = Reuse.by(())(valueA)
  }

  def curry[A, R, S, B](b: R)(
    valueA:                => A
  )(implicit ev:           A =:= ((R, S) => B), typeTagR: TypeTag[R], reuseR: Reusability[R]): Reuse[S => B] =
    ???

  implicit class AppliedFn1Ops[A, R, S, B](aa: Applied[A])(implicit ev: A =:= ((R, S) => B)) {
    // Curry (R, S) => B into (fixed R) + (S => B)
    def apply(
      r:        R
    )(implicit
      typeTagR: TypeTag[R],
      reuseR:   Reusability[R]
    ): Reuse[S => B] =
      Reuse.by(r)(s => ev(aa.value)(r, s))
  }

  implicit class AppliedFn2Ops[A, R, S, T, B](aa: Applied[A])(implicit ev: A =:= ((R, S, T) => B)) {
    // Curry (R, S, T) => B into (fixed R) + ((S, T) => B)
    def apply(
      r:        R
    )(implicit
      typeTagR: TypeTag[R],
      reuseR:   Reusability[R]
    ): Reuse[(S, T) => B] =
      Reuse.by(r)((s, t) => ev(aa.value)(r, s, t))

    // Curry (R, S, T) => B into (fixed (R, S)) + (T => B)
    def apply(
      r:         R,
      s:         S
    )(implicit
      typeTagRS: TypeTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[T => B] =
      Reuse.by((r, s))(t => ev(aa.value)(r, s, t))
  }

  implicit class ReuseFn1Ops[A, R, S, B](ra: Reuse[A])(implicit ev: A =:= ((R, S) => B)) {
    // We can't use "apply" here since it's ambiguous with "toA" which unwraps the function and applies it.

    // Curry (R, S) => B into (fixed R) + (S => B)
    def curry(
      r:        R
    )(implicit
      typeTagR: TypeTag[(ra.B, R)],
      reuseR:   Reusability[R]
    ): Reuse[S => B] = {
      implicit val rB = ra.reusability
      Reuse.by((ra.reuseBy, r))(s => ev(ra.value())(r, s))
    }
  }

  // TODO Convert into Reusable ?? Is it possible?

  object Examples {
    val propsUnits: String = "meters"

    val example1: Double ==> String =
      Reuse((q: Double) => s"$q $propsUnits").by(propsUnits)

    val example2: Double ==> String =
      Reuse.by(propsUnits)(q => s"$q $propsUnits")

    val example3 = Reuse.by(propsUnits)((q: Double) => s"$q $propsUnits")

    def decorate(q: Double): String = s"$q $propsUnits"

    val example4 = Reuse(decorate _).by(propsUnits)

    val example5 = Reuse.by(propsUnits)(decorate _)

    // Currying

    def format(units: String, q: Double): String = s"$q $units"

    val cexample1 = Reuse(format _)("meters")

    def format2(units: String, prefix: String, q: Double): String =
      s"$prefix $q $units"

    val cexample2 = Reuse(format2 _)("meters")

    val cexample3 = Reuse(format2 _)("meters", "Length:")

    val cexample4 = Reuse(format2 _)("meters").curry("Length:")

    val cexample5a = Reuse(format2 _)("meters")
    val cexample5  = cexample5a.curry("Length:")

    val cexample6 = Reuse.curry("meters")(format _)

    import ReuseSyntax._

    val cexample7 = (format _).curry("meters")
  }
}
