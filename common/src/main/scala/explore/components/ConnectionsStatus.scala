// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.components

import clue.PersistentClientStatus
import clue.PersistentClientStatus._
import crystal.Pot
import crystal.react.hooks.*
import explore.*
import explore.components.ui.ExploreStyles
import explore.components.ui.ExploreStyles.*
import explore.syntax.ui.*
import explore.syntax.ui.given
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.ui.syntax.all.*
import lucuma.ui.syntax.all.given
import react.common.ReactFnProps
import react.floatingui.Placement
import react.floatingui.syntax.*
import react.semanticui.elements.icon.*
import react.semanticui.views.item.Item

case class ConnectionsStatus()(using val ctx: AppContextIO)
    extends ReactFnProps[ConnectionsStatus](ConnectionsStatus.component)

object ConnectionsStatus {
  type Props = ConnectionsStatus

  private def renderStatus(name: String, status: Pot[PersistentClientStatus]): VdomNode = {
    val (message, (clazz, show)) = status match {
      case Pot.Pending      => ("Mounting...", (ConnectionWarning, true))
      case Pot.Error(t)     => (t.getMessage, (ConnectionError, true))
      case Pot.Ready(value) =>
        (value.toString,
         value match {
           case Connecting                             => (ConnectionWarning, true)
           case Connected | Initializing | Initialized => (ConnectionOK, false)
           case Disconnected                           => (ConnectionError, true)
         }
        )
    }

    if (show) {
      Item(clazz = ExploreStyles.ConnectionIcon)(
        <.span(Icons.CircleSmall.clazz(clazz)).withTooltip(
          tooltip = message,
          placement = Placement.Bottom
        )
      )
    } else <.span()
  }

  val component =
    ScalaFnComponent
      .withHooks[Props]
      .useStreamOnMountBy(props => props.ctx.clients.odb.statusStream)
      .render((_, status) => renderStatus("ODB", status.toPot))
}
