// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore

import scala.scalajs.js
import js.annotation._
import japgolly.scalajs.react.extra.router._
import explore.model.RootModel
import japgolly.scalajs.react.vdom.VdomElement
import explore.Routing
import japgolly.scalajs.react.vdom.html_<^._
// import gem.Observation

@JSExportTopLevel("Explore")
object ExploreMain extends AppMain {

  override def rootComponent(viewCtx: ViewCtxIO[RootModel]): VdomElement = {
    val routing = new Routing(viewCtx) // !!! This creates a new router on each render.

    // val router = Router(BaseUrl.fromWindowOrigin, routing.config)
    val (router, _) = Router.componentAndCtl(BaseUrl.fromWindowOrigin, routing.config)

    <.div(
      // <.button(
      //   ^.tpe := "button",
      //   routerCtl.setOnClick(ObsPage(Observation.Id.unsafeFromString("GS2020A-Q-1")))
      // )("SET OBS"),
      router()
    )
  }

}
