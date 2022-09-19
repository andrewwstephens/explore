// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.utils

import cats.Eq
import cats.Monoid
import cats.syntax.all._
import coulomb._
import eu.timepit.refined.api._
import lucuma.core.math.units._
import lucuma.core.optics.SplitEpi

def attemptCombine[A: Eq: Monoid, B: Eq: Monoid](a: Option[A], b: Option[B]): Option[(A, B)] =
  (a.filterNot(_.isEmpty), b.filterNot(_.isEmpty)) match
    case (None, None)               => none
    case (Some(someA), None)        => (someA, Monoid[B].empty).some
    case (None, Some(someB))        => (Monoid[A].empty, someB).some
    case (Some(someA), Some(someB)) => (someA, someB).some

// We should move this to lucuma-ui or remove it if there's ever a coulomb-refined for Scala 3
extension [V, U](q: Quantity[V, U]) {
  @inline def toRefined[P](using Validate[V, P]): Either[String, Quantity[V Refined P, U]] =
    refineQV[P](q)
}

abstract class NewType[Wrapped]:
  opaque type Type = Wrapped
  def apply(w: Wrapped): Type = w
  extension (t: Type) def value: Wrapped = t
  given (using CanEqual[Wrapped, Wrapped]): CanEqual[Type, Type] = CanEqual.derived
  given (using eq: Eq[Wrapped]): Eq[Type]                        = eq