// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.observationtree

import cats.effect.IO
import cats.syntax.all._
import clue.TransactionalClient
import crystal.react.implicits._
import explore.AppCtx
import explore.GraphQLSchemas.ObservationDB
import explore.Icons
import explore.components.ui.ExploreStyles
import explore.components.undo.UndoButtons
import explore.components.undo.UndoRegion
import explore.implicits._
import explore.model.ConstraintsSummary
import explore.model.Focused
import explore.model.Focused._
import explore.model.ObsSummary
import explore.optics.GetAdjust
import explore.optics._
import explore.undo.KIListMod
import explore.undo.Undoer
import japgolly.scalajs.react.MonocleReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import lucuma.core.model.ConstraintSet
import lucuma.core.model.Observation
import monocle.Getter
import monocle.function.Field1.first
import monocle.macros.Lenses
import mouse.boolean._
import react.beautifuldnd._
import react.common._
import react.common.implicits._
import react.reflex._
import react.semanticui.elements.header.Header
import react.semanticui.elements.icon.Icon
import react.semanticui.elements.segment.Segment
import react.semanticui.sizes._

import scala.collection.immutable.SortedSet

import ConstraintSetObsQueries._

final case class ConstraintSetObsList(
  constraintSetsWithObs: View[ConstraintSetsWithObs],
  focused:               View[Option[Focused]],
  expandedIds:           View[SortedSet[ConstraintSet.Id]]
) extends ReactProps[ConstraintSetObsList](ConstraintSetObsList.component)
    with ViewCommon {
  override val obsBadgeLayout = ObsBadge.Layout.NameAndConf
}

object ConstraintSetObsList {
  type Props = ConstraintSetObsList

  @Lenses
  case class State(dragging: Boolean = false)

  val obsListMod           = new KIListMod[IO, ObsSummary, Observation.Id](ObsSummary.id)
  val constraintSetListMod =
    new KIListMod[IO, ConstraintsSummary, ConstraintSet.Id](ConstraintsSummary.id)

  class Backend($ : BackendScope[Props, State]) {
    private val UnassignedObsId = "unassignedObs"

    def moveObs(
      obsId:   Observation.Id,
      csIdOpt: Option[ConstraintSet.Id]
    )(implicit
      c:       TransactionalClient[IO, ObservationDB]
    ): IO[Unit] =
      csIdOpt match {
        case Some(csId) => ShareConstraintSetWithObs.execute(csId, obsId).void
        case None       => UnassignConstraintSet.execute(obsId).void
      }

    private def getConstraintSetForObsWithId(
      obsWithIndexGetter: Getter[ObsList, obsListMod.ElemWithIndex]
    ): Getter[ConstraintSetsWithObs, Option[Option[ConstraintsSummary]]] =
      ConstraintSetsWithObs.obs.composeGetter(
        obsWithIndexGetter
          .composeOptionLens(first)
          .composeOptionLens(ObsSummary.constraints)
      )

    private def setConstraintSetForObsWithId(
      constraintSetsWithObs: View[ConstraintSetsWithObs],
      obsId:                 Observation.Id,
      obsWithIndexGetAdjust: GetAdjust[ObsList, obsListMod.ElemWithIndex]
    )(implicit
      c:                     TransactionalClient[IO, ObservationDB]
    ): Option[Option[ConstraintsSummary]] => IO[Unit] = { csOpt =>
      val obsCsAdjuster = obsWithIndexGetAdjust
        .composeOptionLens(first)
        .composeOptionLens(ObsSummary.constraints)

      val observationsView = constraintSetsWithObs.zoom(ConstraintSetsWithObs.obs)

      // 1) Update internal model
      observationsView.mod(obsCsAdjuster.set(csOpt)) >>
        // 2) Send mutation
        csOpt.map(newCs => moveObs(obsId, newCs.map(_.id))).orEmpty

    }

    protected def onDragEnd(
      setter:      Undoer.Setter[IO, ConstraintSetsWithObs],
      expandedIds: View[SortedSet[ConstraintSet.Id]]
    )(implicit
      c:           TransactionalClient[IO, ObservationDB]
    ): (DropResult, ResponderProvided) => IO[Unit] =
      (result, _) =>
        $.propsIn[IO] >>= { props =>
          result.destination.toOption
            .map(destination =>
              result.draggableId match {
                case Observation.Id(obsId) =>
                  val obsWithId: GetAdjust[ObsList, obsListMod.ElemWithIndex] =
                    obsListMod.withKey(obsId)

                  val set: Option[Option[ConstraintsSummary]] => IO[Unit] =
                    setter.set[Option[Option[ConstraintsSummary]]](
                      props.constraintSetsWithObs.get,
                      getConstraintSetForObsWithId(obsWithId.getter).get,
                      setConstraintSetForObsWithId(props.constraintSetsWithObs, obsId, obsWithId)
                    )

                  def getSummary(csId: ConstraintSet.Id): IO[ConstraintsSummary] = {
                    val csEither: Either[Throwable, ConstraintsSummary] = constraintSetListMod
                      .getterForKey(csId)
                      .get(props.constraintSetsWithObs.get.constraintSets)
                      .map(_._1)
                      .toRight(new Exception("Not found"))
                    IO.fromEither(csEither)
                  }

                  destination.droppableId match {
                    case UnassignedObsId           => set(none.some)
                    case ConstraintSet.Id(newCsId) =>
                      getSummary(newCsId).flatMap(cs =>
                        expandedIds.mod(_ + newCsId) >>
                          set(cs.some.some)
                      )
                    case _                         => IO.unit
                  }
                case _                     => IO.unit
              }
            )
            .orEmpty
        }

    def toggleExpanded(
      id:          ConstraintSet.Id,
      expandedIds: View[SortedSet[ConstraintSet.Id]]
    ): IO[Unit] =
      expandedIds
        .mod(expanded => expanded.exists(_ === id).fold(expanded - id, expanded + id))

    def render(props: Props, state: State): VdomElement = AppCtx.runWithCtx { implicit ctx =>
      val observations       = props.constraintSetsWithObs.get.obs
      val obsByConstraintSet = observations.toList.groupBy(_.constraints.map(_.id))

      val constraintSets        = props.constraintSetsWithObs.get.constraintSets
      val constraintSetsWithIdx = constraintSets.toList.zipWithIndex

      val unassignedObs = obsByConstraintSet.get(none).orEmpty

      val renderClone: Draggable.Render =
        (provided, snapshot, rubric) => {
          <.div(provided.innerRef,
                provided.draggableProps,
                provided.dragHandleProps,
                props.getDraggedStyle(provided.draggableStyle, snapshot)
          )(
            (rubric.draggableId match {
              case Observation.Id(obsId) => observations.getElement(obsId).map(props.renderObsBadge)
              case _                     => none
            }).getOrElse(<.span("ERROR"))
          )
        }

      UndoRegion[ConstraintSetsWithObs] { undoCtx =>
        val handleDragEnd = onDragEnd(undoCtx.setter, props.expandedIds)

        DragDropContext(
          onDragStart = (_: DragStart, _: ResponderProvided) => $.setStateL(State.dragging)(true),
          onDragEnd = (result, provided) =>
            $.setStateL(State.dragging)(false) >> handleDragEnd(result, provided).runAsyncCB
        )(
          <.div(ExploreStyles.ObsTreeWrapper)(
            <.div(ExploreStyles.TreeToolbar)(
              <.span(), // Add button goes here.
              UndoButtons(props.constraintSetsWithObs.get, undoCtx, size = Mini)
            ),
            ReflexContainer()(
              List[VdomNode](
                // Start constraint sets tree
                (ReflexElement(minSize = 36, clazz = ExploreStyles.ObsTreeSection)(
                  Header(block = true, clazz = ExploreStyles.ObsTreeHeader)("CONSTRAINTS"),
                  <.div(ExploreStyles.ObsTree)(
                    <.div(ExploreStyles.ObsScrollTree)(
                      constraintSetsWithIdx.toTagMod { case (constraintSet, _) =>
                        val csId = constraintSet.id
                        // will be needed in the next PR
                        // val nextToSelect  = constraintSetsWithIdx.find(_._2 === csIdx + 1).map(_._1)
                        // val prevToSelect  = constraintSetsWithIdx.find(_._2 === csIdx - 1).map(_._1)
                        // val focusOnDelete = nextToSelect.orElse(prevToSelect)

                        val csObs = obsByConstraintSet.get(csId.some).orEmpty

                        val opIcon = csObs.nonEmpty.fold(
                          Icon(
                            "chevron " + props.expandedIds.get
                              .exists(_ === csId)
                              .fold("down", "right")
                          )(^.cursor.pointer,
                            ^.onClick ==> { e: ReactEvent =>
                              e.stopPropagationCB >>
                                toggleExpanded(csId, props.expandedIds).runAsyncCB
                                  .asEventDefault(e)
                                  .void
                            }
                          ),
                          Icons.ChevronRight
                        )

                        val obsSelected = props.focused.get
                          .exists(f => csObs.map(obs => FocusedObs(obs.id)).exists(f === _))

                        Droppable(csId.toString, renderClone = renderClone) {
                          case (provided, snapshot) =>
                            val csHeader = <.span(ExploreStyles.ObsTreeGroupHeader)(
                              // TODO: Give it its own style?
                              <.span(ExploreStyles.TargetLabelTitle)(
                                opIcon,
                                constraintSet.name.value
                              ), // delete button goes here
                              <.span(ExploreStyles.ObsCount, s"${csObs.length} Obs")
                            )

                            <.div(
                              provided.innerRef,
                              provided.droppableProps,
                              props.getListStyle(
                                snapshot.draggingOverWith.exists(id =>
                                  Observation.Id.parse(id).isDefined
                                )
                              )
                            )(
                              Segment(
                                vertical = true,
                                clazz = ExploreStyles.ObsTreeGroup
                                  |+| Option
                                    .when(
                                      obsSelected || props.focused.get
                                        .exists(_ === FocusedConstraintSet(csId))
                                    )(ExploreStyles.SelectedObsTreeGroup)
                                    .orElse(
                                      Option
                                        .when(!state.dragging)(ExploreStyles.UnselectedObsTreeGroup)
                                    )
                                    .orEmpty
                              )(
                                ^.cursor.pointer,
                                ^.onClick --> props.focused
                                  .set(FocusedConstraintSet(csId).some)
                                  .runAsyncCB
                              )(
                                csHeader,
                                TagMod.when(props.expandedIds.get.contains(csId))(
                                  csObs.zipWithIndex.toTagMod(
                                    (props.renderObsBadgeItem _).tupled
                                  )
                                ),
                                <.span(provided.placeholder)
                              )
                            )
                        }
                      }
                    )
                  )
                ): VdomNode),
                // end of constraint set tree
                (ReflexSplitter(propagate = true): VdomNode),
                // start of unassigned observations list
                (
                  ReflexElement(size = 36, minSize = 36, clazz = ExploreStyles.ObsTreeSection)(
                    ReflexHandle()(
                      Header(block = true,
                             clazz =
                               ExploreStyles.ObsTreeHeader |+| ExploreStyles.ObsTreeGroupHeader
                      )(
                        <.span(ExploreStyles.TargetLabelTitle)("UNASSIGNED OBSERVATIONS"),
                        <.span(ExploreStyles.ObsCount, s"${unassignedObs.length} Obs")
                      )
                    ),
                    Droppable(UnassignedObsId) { case (provided, snapshot) =>
                      <.div(
                        provided.innerRef,
                        provided.droppableProps,
                        props.getListStyle(snapshot.isDraggingOver)
                      )(
                        <.div(ExploreStyles.ObsTree)(
                          <.div(ExploreStyles.ObsScrollTree) {
                            Segment(
                              vertical = true,
                              clazz = ExploreStyles.ObsTreeGroup
                            )(
                              unassignedObs.zipWithIndex.toTagMod(
                                (props.renderObsBadgeItem _).tupled
                              ),
                              provided.placeholder
                            )
                          }
                        )
                      )
                    }
                  ): VdomNode
                  // end of unassigned observations list
                )
              ).toVdomArray
            )
          )
        )
      }
    }
  }

  protected val component =
    ScalaComponent
      .builder[Props]
      .initialState(State())
      .renderBackend[Backend]
      .componentDidMount { $ =>
        AppCtx.runWithCtx { implicit ctx =>
          val constraintSetsWithObs = $.props.constraintSetsWithObs.get
          val expandedIds           = $.props.expandedIds

          // expand constraint set with focused observation
          val expandCs = $.props.focused.get
            .collect { case FocusedObs(obsId) =>
              constraintSetsWithObs.obs
                .getElement(obsId)
                .flatMap(_.constraints.map(c => expandedIds.mod(_ + c.id)))
            }
            .flatten
            .orEmpty

          // Remove contraint sets from expanded list which no longer exist.
          val removeConstraintSets =
            (expandedIds.get -- constraintSetsWithObs.constraintSets.toList.map(_.id)).toNes
              .map(missingIds => expandedIds.mod(_ -- missingIds.toSortedSet))
              .orEmpty

          (expandCs >> removeConstraintSets).runAsyncCB
        }
      }
      .build
}
