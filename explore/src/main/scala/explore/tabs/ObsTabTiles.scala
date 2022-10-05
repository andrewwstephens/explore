// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.tabs

import cats.effect.IO
import cats.syntax.all.*
import clue.TransactionalClient
import crystal.Pot
import crystal.react.*
import crystal.react.hooks.*
import crystal.react.implicits.*
import explore.*
import explore.components.Tile
import explore.components.TileController
import explore.components.ui.ExploreStyles
import explore.model.AppContext
import explore.model.Asterism
import explore.model.ConstraintGroup
import explore.model.CoordinatesAtVizTime
import explore.model.Focused
import explore.model.GridLayoutSection
import explore.model.ModelUndoStacks
import explore.model.ObsIdSet
import explore.model.ScienceMode
import explore.model.TargetSummary
import explore.model.display.given
import explore.model.enums.AppTab
import explore.model.itc.ItcChartExposureTime
import explore.model.itc.ItcTarget
import explore.model.itc.OverridenExposureTime
import explore.model.layout.*
import explore.optics.*
import explore.optics.all.*
import explore.undo.UndoStacks
import explore.utils.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.SetRouteVia
import japgolly.scalajs.react.vdom.html_<^.*
import lucuma.core.math.Coordinates
import lucuma.core.model.Observation
import lucuma.core.model.Program
import lucuma.core.model.Target
import lucuma.core.model.User
import lucuma.core.syntax.all.*
import lucuma.schemas.ObservationDB
import lucuma.ui.syntax.all.*
import lucuma.ui.syntax.all.given
import queries.common.ObsQueriesGQL.*
import queries.schemas.odb.ObsQueries
import queries.schemas.odb.ObsQueries.*
import react.common.ReactFnProps
import react.semanticui.addons.select.Select
import react.semanticui.addons.select.Select.SelectItem
import react.semanticui.modules.dropdown.Dropdown

import java.time.Instant
import scala.collection.immutable.SortedMap

case class ObsTabTiles(
  userId:           Option[User.Id],
  programId:        Program.Id,
  obsId:            Observation.Id,
  backButton:       VdomNode,
  constraintGroups: View[ConstraintsList],
  focusedObs:       Option[Observation.Id],
  focusedTarget:    Option[Target.Id],
  targetMap:        SortedMap[Target.Id, TargetSummary],
  undoStacks:       View[ModelUndoStacks[IO]],
  searching:        View[Set[Target.Id]],
  hiddenColumns:    View[Set[String]],
  defaultLayouts:   LayoutsMap,
  layouts:          View[Pot[LayoutsMap]],
  coreWidth:        Int,
  coreHeight:       Int
) extends ReactFnProps(ObsTabTiles.component)

object ObsTabTiles:
  private type Props = ObsTabTiles

  private def makeConstraintsSelector(
    constraintGroups: View[ConstraintsList],
    obsView:          Pot[View[ObsEditData]]
  )(using TransactionalClient[IO, ObservationDB]): VdomNode =
    potRender[View[ObsEditData]] { vod =>
      val cgOpt: Option[ConstraintGroup] =
        constraintGroups.get.find(_._1.contains(vod.get.id)).map(_._2)

      Select(
        clazz = ExploreStyles.ConstraintsTileSelector,
        value = cgOpt.map(cg => ObsIdSet.fromString.reverseGet(cg.obsIds)).orEmpty,
        onChange = (p: Dropdown.DropdownProps) => {
          val newCgOpt =
            ObsIdSet.fromString
              .getOption(p.value.toString)
              .flatMap(ids => constraintGroups.get.get(ids))
          newCgOpt.map { cg =>
            vod
              .zoom(ObsEditData.scienceData.andThen(ScienceData.constraints))
              .set(cg.constraintSet) >>
              ObsQueries
                .updateObservationConstraintSet[IO](List(vod.get.id), cg.constraintSet)
                .runAsyncAndForget
          }.getOrEmpty
        },
        options = constraintGroups.get
          .map(kv =>
            new SelectItem(
              value = ObsIdSet.fromString.reverseGet(kv._1),
              text = kv._2.constraintSet.shortName
            )
          )
          .toList
      )
    }(obsView)

  private def otherObsCount(
    targetObsMap: SortedMap[Target.Id, TargetSummary],
    obsId:        Observation.Id,
    targetId:     Target.Id
  ): Int =
    targetObsMap.get(targetId).fold(0)(summary => (summary.obsIds - obsId).size)

  private val component =
    ScalaFnComponent
      .withHooks[Props]
      .useContext(AppContext.ctx)
      .useStreamResourceViewOnMountBy { (props, ctx) =>
        import ctx.given

        ObsEditQuery
          .query(props.obsId)
          .map(
            _.asObsEditData
              .getOrElse(throw new Exception(s"Observation [${props.obsId}] not found"))
          )
          .reRunOnResourceSignals(ObservationEditSubscription.subscribe[IO](props.obsId))
      }
      // ITC selected target. Here to be shared by the ITC tile body and title
      .useStateView(none[ItcTarget])
      .render { (props, ctx, obsView, itcTarget) =>
        import ctx.given

        val obsViewPot = obsView.toPot

        val scienceMode: Option[ScienceMode] =
          obsView.toOption.flatMap(_.get.scienceData.mode)

        val posAngle = obsView.toOption.flatMap(_.get.scienceData.posAngle)

        val potAsterism: Pot[View[Option[Asterism]]] =
          obsViewPot.map(v =>
            v.zoom(
              ObsEditData.scienceData
                .andThen(ScienceData.targets)
                .andThen(ObservationData.TargetEnvironment.asterism)
            ).zoom(Asterism.fromTargetsListOn(props.focusedTarget).asLens)
          )

        val potAsterismMode: Pot[(View[Option[Asterism]], Option[ScienceMode])] =
          potAsterism.map(x => (x, scienceMode))

        val vizTimeView: Pot[View[Option[Instant]]] =
          obsViewPot.map(_.zoom(ObsEditData.visualizationTime))

        val vizTime = vizTimeView.toOption.flatMap(_.get)

        // asterism base coordinates at viz time or default to base coordinates
        val targetCoords: Option[CoordinatesAtVizTime] =
          (vizTime, potAsterism.toOption)
            .mapN((instant, asterism) => asterism.get.flatMap(_.baseTracking.at(instant)))
            .flatten
            .orElse(
              // If e.g. vizTime isn't defined default to the asterism base coordinates
              potAsterism.toOption
                .flatMap(_.get.map(x => CoordinatesAtVizTime(x.baseTracking.baseCoordinates)))
            )

        val spectroscopyReqs: Option[ScienceRequirementsData] =
          obsView.toOption.map(_.get.scienceData.requirements)

        val notesTile =
          Tile(
            ObsTabTilesIds.NotesId.id,
            s"Note for Observer",
            props.backButton.some,
            canMinimize = true
          )(_ =>
            <.div(
              ExploreStyles.NotesWrapper,
              <.div(
                ExploreStyles.ObserverNotes,
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus maximus hendrerit lacinia. Etiam dapibus blandit ipsum sed rhoncus."
              )
            )
          )

        val constraints =
          obsViewPot.map(_.zoom(ObsEditData.scienceData.andThen(ScienceData.constraints)))

        val scienceData = obsViewPot.toOption.map(a => ObsEditData.scienceData.get(a.get))
        println(
          obsView.toOption
            .flatMap(
              _.get.itcExposureTime
                .map(r => ItcChartExposureTime(OverridenExposureTime.FromItc, r.time, r.count))
            )
        )

        val itcTile =
          ItcTile.itcTile(
            props.userId,
            props.obsId,
            scienceMode,
            obsView.toOption.map(_.get.scienceData.requirements.spectroscopy),
            scienceData,
            obsView.toOption
              .flatMap(
                _.get.itcExposureTime
                  .map(r => ItcChartExposureTime(OverridenExposureTime.FromItc, r.time, r.count))
              ),
            itcTarget
          )

        val constraintsSelector = makeConstraintsSelector(props.constraintGroups, obsViewPot)

        // first target of the obs. We can use it in case there is no target focus
        val firstTarget = props.targetMap.collect {
          case (tid, ts) if props.focusedObs.forall(o => ts.obsIds.contains(o)) => tid
        }.headOption

        val skyPlotTile =
          ElevationPlotTile.elevationPlotTile(
            props.userId,
            props.focusedTarget.orElse(firstTarget),
            scienceMode,
            targetCoords,
            vizTime
          )

        def setCurrentTarget(programId: Program.Id, oid: Option[Observation.Id])(
          tid:                          Option[Target.Id],
          via:                          SetRouteVia
        ): Callback =
          (potAsterism.toOption, tid)
            // When selecting the current target focus the asterism zipper
            .mapN((pot, tid) => pot.mod(_.map(_.focusOn(tid))))
            .getOrEmpty *>
            // Set the route base on the selected target
            ctx.setPageVia(
              AppTab.Observations,
              programId,
              Focused(oid.map(ObsIdSet.one), tid),
              via
            )

        val targetTile = AsterismEditorTile.asterismEditorTile(
          props.userId,
          props.programId,
          ObsIdSet.one(props.obsId),
          potAsterismMode,
          vizTimeView,
          posAngle,
          obsView.toOption.map(_.get.scienceData.constraints),
          obsView.toOption.flatMap(_.get.scienceData.requirements.spectroscopy.wavelength),
          props.focusedTarget,
          setCurrentTarget(props.programId, props.focusedObs),
          otherObsCount(props.targetMap, props.obsId, _),
          props.undoStacks.zoom(ModelUndoStacks.forSiderealTarget),
          props.searching,
          "Targets",
          none,
          props.hiddenColumns
        )

        // The ExploreStyles.ConstraintsTile css adds a z-index to the constraints tile react-grid wrapper
        // so that the constraints selector dropdown always appears in front of any other tiles. If more
        // than one tile ends up having dropdowns in the tile header, we'll need something more complex such
        // as changing the css classes on the various tiles when the dropdown is clicked to control z-index.
        val constraintsTile =
          ConstraintsTile.constraintsTile(
            props.obsId,
            constraints,
            props.undoStacks
              .zoom(ModelUndoStacks.forConstraintGroup[IO])
              .zoom(atMapWithDefault(ObsIdSet.one(props.obsId), UndoStacks.empty)),
            control = constraintsSelector.some,
            clazz = ExploreStyles.ConstraintsTile.some
          )

        val configurationTile =
          ConfigurationTile.configurationTile(
            props.obsId,
            obsViewPot.map(obsEditData =>
              (obsEditData.get.title,
               obsEditData.get.subtitle,
               obsEditData.zoom(ObsEditData.scienceData)
              )
            ),
            props.undoStacks
              .zoom(ModelUndoStacks.forObservationData[IO])
              .zoom(atMapWithDefault(props.obsId, UndoStacks.empty)),
            targetCoords
          )

        val rglRender: LayoutsMap => VdomNode = (l: LayoutsMap) =>
          TileController(
            props.userId,
            props.coreWidth,
            props.defaultLayouts,
            l,
            List(
              notesTile,
              targetTile,
              skyPlotTile,
              constraintsTile,
              configurationTile,
              itcTile
            ),
            GridLayoutSection.ObservationsLayout,
            clazz = ExploreStyles.ObservationTiles.some
          )

        potRenderView[LayoutsMap](rglRender)(props.layouts)
      }
