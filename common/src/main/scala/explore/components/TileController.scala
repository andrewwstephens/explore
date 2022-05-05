// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.components

import cats.effect.IO
import cats.syntax.all._
import crystal.react.ReuseView
import crystal.react.hooks._
import crystal.react.implicits._
import crystal.react.reuse._
import eu.timepit.refined.auto._
import explore.common.UserPreferencesQueries._
import explore.components.ui.ExploreStyles
import explore.implicits._
import explore.model.Constants
import explore.model.GridLayoutSection
import explore.model.enum.TileSizeState
import explore.model.layout._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import lucuma.core.model.User
import lucuma.ui.reusability._
import monocle.Traversal
import queries.common.UserPreferencesQueriesGQL._
import react.common._
import react.common.implicits._
import react.common.style.Css
import react.gridlayout._

import scala.concurrent.duration._
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|

final case class TileController(
  userId:           Option[User.Id],
  gridWidth:        Int,
  defaultLayout:    LayoutsMap,
  layoutMap:        ReuseView[LayoutsMap],
  tiles:            List[Tile],
  section:          GridLayoutSection,
  clazz:            Option[Css] = None
)(implicit val ctx: AppContextIO)
    extends ReactFnProps[TileController](TileController.component)

object TileController {
  type Props = TileController

  implicit val propsReuse: Reusability[TileController] = Reusability.derive

  def storeLayouts(
    userId:    Option[User.Id],
    section:   GridLayoutSection,
    layouts:   Layouts,
    debouncer: Reusable[UseSingleEffect[IO]]
  )(implicit
    ctx:       AppContextIO
  ): Callback =
    debouncer
      .submit(UserGridLayoutUpsert.storeLayoutsPreference[IO](userId, section, layouts))
      .runAsyncAndForget

  val itemHeght = layoutItems.andThen(layoutItemHeight)

  // Calculate the state out of the height
  def unsafeSizeToState(
    layoutsMap: LayoutsMap,
    tileId:     Tile.TileId
  ): TileSizeState = {
    val k = allTiles
      .filter((s => s.i.forall(_ === tileId.value)))
      .getAll(layoutsMap)
      .headOption

    val h = k.map(layoutItemHeight.get)
    if (h === Some(1)) TileSizeState.Minimized else TileSizeState.Normal
  }

  val allTiles: Traversal[LayoutsMap, LayoutItem] =
    allLayouts.andThen(layoutItems)

  def unsafeTileHeight(id: Tile.TileId): Traversal[LayoutsMap, Int] =
    allTiles
      .filter(_.i.forall(_ === id.value))
      .andThen(layoutItemHeight)

  def tileResizable(id: Tile.TileId): Traversal[LayoutsMap, Boolean | Unit] =
    allTiles
      .filter(_.i.forall(_ === id.value))
      .andThen(layoutItemResizable)

  def updateResizableState(p: LayoutsMap): LayoutsMap =
    allLayouts
      .andThen(layoutItems)
      .modify {
        case r if r.h === 1 => r.copy(isResizable = false)
        case r              => r
      }(p)

  val component =
    ScalaFnComponent
      .withHooks[Props]
      .useSingleEffect(debounce = 1.second)
      // Store the current breakpoint
      .useStateBy { (p, _) =>
        getBreakpointFromWidth(p.layoutMap.get.map { case (x, (w, _, _)) => x -> w }, p.gridWidth)
      }
      // Store the current layout
      .useStateBy((p, _, _) => p.layoutMap.get)
      .renderWithReuse { (p, debouncer, bn, currentLayout) =>
        def sizeState(id: Tile.TileId, st: TileSizeState): Callback =
          p.layoutMap
            .zoom(allTiles)
            .mod {
              case l if l.i.forall(_ === id.value) =>
                if (st === TileSizeState.Minimized)
                  l.copy(h = 1, minH = 1, isResizable = false)
                else if (st === TileSizeState.Normal) {
                  val defaultHeight = unsafeTileHeight(id).headOption(p.defaultLayout).getOrElse(1)
                  // restore the resizable state
                  val resizable     =
                    tileResizable(id).headOption(p.defaultLayout).getOrElse(true: Boolean | Unit)
                  // TODO: Restore to the previous size
                  l.copy(h = defaultHeight,
                         isResizable = resizable,
                         minH = scala.math.max(l.minH.getOrElse(1), defaultHeight)
                  )
                } else l
              case l                               => l
            }

        val layouts = updateResizableState(p.layoutMap.get)

        ResponsiveReactGridLayout(
          width = p.gridWidth.toDouble,
          autoSize = true,
          margin = (Constants.GridRowPadding, Constants.GridRowPadding),
          containerPadding = (Constants.GridRowPadding, 0),
          rowHeight = Constants.GridRowHeight,
          draggableHandle = s".${ExploreStyles.TileTitleMenu.htmlClass}",
          onBreakpointChange = (bk: BreakpointName, _: Int) => bn.setState(bk),
          onLayoutChange = (_: Layout, b: Layouts) =>
            {
              val le = b.layouts.find(_.name.name === bn.value.name).map(_.layout)

              // Store the current layout in the state for debugging
              le.map { l =>
                currentLayout.modState(breakpointLayout(bn.value).replace(l))
              }
            }.getOrEmpty *>
              storeLayouts(p.userId, p.section, b, debouncer)(p.ctx),
          layouts = layouts,
          className = p.clazz.map(_.htmlClass).orUndefined
        )(
          p.tiles.map { t =>
            <.div(
              ^.key := t.id.value,
              // Show tile proprties on the title if enabled
              currentLayout.value
                .get(bn.value)
                .flatMap { case (_, _, l) =>
                  l.l
                    .find(_.i === t.id.value)
                    .flatMap { i =>
                      TagMod
                        .devOnly(
                          <.div(
                            ^.cls := "rgl-tile-overlay",
                            s"id: ${i.i} x: ${i.x} y: ${i.y} w: ${i.w} h: ${i.h}${i.minH.toOption
                                .foldMap(m => s" minH: $m")}${i.maxH.toOption
                                .foldMap(m => s" maxH: $m")}${i.minW.toOption
                                .foldMap(m => s" minW: $m")}${i.maxW.toOption
                                .foldMap(m => s" maxW: $m")}"
                          )
                        )
                        .some
                    }
                }
                .getOrElse(EmptyVdom),
              t.controllerClass.orEmpty,
              t.withState(unsafeSizeToState(p.layoutMap.get, t.id), Reuse(sizeState _)(t.id))
            )
          }.toVdomArray
        )
      }

}
