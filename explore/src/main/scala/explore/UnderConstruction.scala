// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore

import explore.Icons
import explore.components.ui.ExploreStyles
import explore.syntax.ui.*
import explore.syntax.ui.given
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.ui.syntax.all.*
import lucuma.ui.syntax.all.given
import react.fa.Flip
import react.fa.IconSize
import react.fa.given

object UnderConstruction {

  protected val component =
    ScalaComponent
      .builder[Unit]
      .stateless
      .render { _ =>
        <.div(
          ExploreStyles.HVCenter,
          <.div(
            <.div("Under Construction"),
            <.div(ExploreStyles.HVCenter,
                  Icons.Gears
                    .withSize(IconSize.X5)
                    .withTitle("Under construction")
            )
          )
        )
      }
      .build

  def apply() = component()

}
