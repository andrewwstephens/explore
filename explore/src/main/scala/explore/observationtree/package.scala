// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.observationtree

import cats.effect.IO
import cats.syntax.all.*
import clue.TransactionalClient
import clue.data.syntax.*
import crystal.react.implicits.*
import explore.data.KeyedIndexedList
import explore.model.AppContext
import explore.model.Focused
import explore.model.ObsSummaryWithTitleAndConstraints
import explore.model.ObsSummaryWithTitleConstraintsAndConf
import explore.model.enums.AppTab
import explore.optics.GetAdjust
import explore.optics.all.*
import explore.undo.Action
import explore.undo.KIListMod
import explore.undo.UndoContext
import japgolly.scalajs.react.*
import lucuma.core.model.Observation
import lucuma.core.model.Program
import lucuma.schemas.ObservationDB
import lucuma.schemas.ObservationDB.Types.*
import monocle.Focus
import queries.common.ObsQueriesGQL.*
import queries.schemas.odb.ODBConversions.*
import queries.schemas.odb.ObsQueries.*

val obsListMod =
  KIListMod[ObsSummaryWithTitleConstraintsAndConf, Observation.Id](
    ObsSummaryWithTitleConstraintsAndConf.id
  )

def setObs[F[_]](
  programId: Program.Id,
  obsId:     Option[Observation.Id],
  ctx:       AppContext[F]
): Callback =
  ctx.pushPage(AppTab.Observations, programId, obsId.fold(Focused.None)(Focused.singleObs(_)))

def cloneObs(
  programId: Program.Id,
  obsId:     Observation.Id,
  pos:       Int,
  undoCtx:   UndoContext[ObservationList],
  ctx:       AppContext[IO],
  before:    IO[Unit] = IO.unit,
  after:     IO[Unit] = IO.unit
): IO[Unit] =
  import ctx.given

  before >>
    cloneObservation[IO](obsId)
      .flatMap { obs =>
        obsExistence(obs.id, o => setObs(programId, o.some, ctx))
          .mod(undoCtx)(obsListMod.upsert(obs.toTitleAndConstraints, pos))
          .to[IO]
      }
      .guarantee(after)

private def obsWithId(
  obsId: Observation.Id
): GetAdjust[KeyedIndexedList[Observation.Id, ObsSummaryWithTitleConstraintsAndConf], Option[
  ObsSummaryWithTitleConstraintsAndConf
]] =
  obsListMod
    .withKey(obsId)
    .composeOptionLens(Focus[(ObsSummaryWithTitleConstraintsAndConf, Int)](_._1))

def obsEditStatus(obsId: Observation.Id)(using TransactionalClient[IO, ObservationDB]) = Action(
  access = obsWithId(obsId).composeOptionLens(ObsSummaryWithTitleConstraintsAndConf.status)
)(onSet =
  (_, status) =>
    UpdateObservationMutation
      .execute[IO](
        UpdateObservationsInput(
          WHERE = obsId.toWhereObservation.assign,
          SET = ObservationPropertiesInput(status = status.orIgnore)
        )
      )
      .void
)

def obsEditSubtitle(obsId: Observation.Id)(using TransactionalClient[IO, ObservationDB]) = Action(
  access = obsWithId(obsId).composeOptionLens(ObsSummaryWithTitleConstraintsAndConf.subtitle)
)(onSet =
  (_, subtitleOpt) =>
    UpdateObservationMutation
      .execute[IO](
        UpdateObservationsInput(
          WHERE = obsId.toWhereObservation.assign,
          SET = ObservationPropertiesInput(subtitle = subtitleOpt.flatten.orUnassign)
        )
      )
      .void
)

def obsActiveStatus(obsId: Observation.Id)(using TransactionalClient[IO, ObservationDB]) = Action(
  access = obsWithId(obsId).composeOptionLens(ObsSummaryWithTitleConstraintsAndConf.activeStatus)
)(onSet =
  (_, activeStatus) =>
    UpdateObservationMutation
      .execute[IO](
        UpdateObservationsInput(
          WHERE = obsId.toWhereObservation.assign,
          SET = ObservationPropertiesInput(activeStatus = activeStatus.orIgnore)
        )
      )
      .void
)

def obsExistence(obsId: Observation.Id, setObs: Observation.Id => Callback)(using
  TransactionalClient[IO, ObservationDB]
) =
  Action(
    access = obsListMod.withKey(obsId)
  )(
    onSet = (_, elemWithIndexOpt) =>
      elemWithIndexOpt.fold {
        deleteObservation[IO](obsId)
      } { case (obs, _) =>
        // Not much to do here, the observation must be created before we get here
        setObs(obs.id).to[IO]
      },
    onRestore = (_, elemWithIndexOpt) =>
      elemWithIndexOpt.fold {
        deleteObservation[IO](obsId)
      } { case (obs, _) =>
        undeleteObservation[IO](obs.id) >>
          setObs(obs.id).to[IO]
      }
  )