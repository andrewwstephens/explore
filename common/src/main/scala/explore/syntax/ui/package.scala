// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.syntax

import cats.Eq
import cats.syntax.all._
import explore.components.InputWithUnits
import explore.components.ui.ExploreStyles
import explore.model.Constants
import explore.utils._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.html_<^._
import lucuma.ui.forms.ExternalValue
import lucuma.ui.forms.FormInputEV
import org.scalajs.dom.Window
import react.common.Css
import react.common.GenericComponentPAC
import react.common.GenericFnComponentPA
import react.common.GenericFnComponentPAC
import react.common.GenericFnComponentPC
import react.common.implicits._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

package object ui {
  implicit class WindowOps(val self: Window) extends AnyVal {
    def canFitTwoPanels: Boolean =
      self.innerWidth <= Constants.TwoPanelCutoff
  }

  implicit class FormInputEVOps[EV[_], A, B](val input: FormInputEV[EV, Option[A]]) extends AnyVal {
    def clearable(implicit ev: ExternalValue[EV], ev3: Eq[A]) =
      input.copy(icon = clearInputIcon[EV, A](input.value))

    // When an icon is added to a FormInputEV, SUI adds extra padding on the right to make
    // space for the icon. However, with some layouts this can cause resizing issues, so this
    // method removes that extra padding. See `clearInputIcon` for more details.
    def clearableNoPadding(implicit ev: ExternalValue[EV], ev3: Eq[A]) = {
      val newClazz: UndefOr[Css] =
        input.clazz.fold(ExploreStyles.ClearableInputPaddingReset)(
          _ |+| ExploreStyles.ClearableInputPaddingReset
        )
      input.copy(icon = clearInputIcon[EV, A](input.value), clazz = newClazz)
    }
  }

  implicit class InputWithUnitsOps[EV[_], A, B](val input: InputWithUnits[EV, Option[A]])
      extends AnyVal {
    def clearable(implicit ev: ExternalValue[EV], ev3: Eq[A]) =
      input.copy(icon = clearInputIcon[EV, A](input.value))

    // When an icon is added to a FormInputEV, SUI adds extra padding on the right to make
    // space for the icon. However, with some layouts this can cause resizing issues, so this
    // method removes that extra padding. See `clearInputIcon` for more details.
    def clearableNoPadding(implicit ev: ExternalValue[EV], ev3: Eq[A]) = {
      val newClazz = input.clazz |+| ExploreStyles.ClearableInputPaddingReset
      input.copy(icon = clearInputIcon[EV, A](input.value), clazz = newClazz)
    }
  }

  // Conversion of common components to VdomNode
  type FnPA[P <: js.Object] = GenericFnComponentPA[P, ?]
  given Conversion[FnPA[?], UndefOr[VdomNode]] = _.render
  given Conversion[FnPA[?], VdomNode]          = _.render

  type FnPAC[P <: js.Object] = GenericFnComponentPAC[P, ?]
  given Conversion[FnPAC[?], UndefOr[VdomNode]] = _.render
  given Conversion[FnPAC[?], VdomNode]          = _.render

  given Conversion[Css, TagMod] =
    ^.className := _.htmlClass

  type ClassPAC[P <: js.Object] = GenericComponentPAC[P, ?]
  // Without the explicit `vdomElement` this produces a compiler exception
  given Conversion[ClassPAC[?], UndefOr[VdomNode]] = _.render.vdomElement
  given Conversion[ClassPAC[?], VdomNode]          = _.render.vdomElement

  // Syntaxis for apply
  extension [P <: js.Object, A](c: GenericFnComponentPC[P, A])
    def apply(children: VdomNode*): A = c.withChildren(children)

  extension [P <: js.Object, A](c: GenericFnComponentPA[P, A])
    def apply(modifiers: TagMod*): A = c.addModifiers(modifiers)

  extension [P <: js.Object, A](c: GenericFnComponentPAC[P, A])
    def apply(modifiers: TagMod*): A = c.addModifiers(modifiers)
}
