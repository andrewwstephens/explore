// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.model

import clue.PersistentClientStatus
import crystal.react.implicits._
import explore.data.KeyedIndexedList
import explore.undo.UndoContext
import explore.undo.UndoSetter
import explore.undo.UndoStacks
import japgolly.scalajs.react.ReactCats._
import japgolly.scalajs.react.Reusability
import lucuma.core.model.Semester
import lucuma.ui.reusability._
import react.common.style.Css
import lucuma.core.model.EphemerisKey
import lucuma.core.model.NonsiderealTarget
import lucuma.core.model.SiderealTarget
import lucuma.core.model.Target
import scala.annotation.unused
import scala.collection.immutable.TreeSeqMap

/**
 * Reusability instances for model classes
 */
object reusability {
  // Model
  implicit val targetSummaryReuse: Reusability[TargetSummary]                                 = Reusability.derive
  implicit val statusReuse: Reusability[PersistentClientStatus]                               = Reusability.derive
  implicit val targetOptionsReuse: Reusability[TargetVisualOptions]                           = Reusability.derive
  implicit val userVaultReuse: Reusability[UserVault]                                         = Reusability.derive
  implicit val targetViewExpandedIdsReuse: Reusability[ExpandedIds]                           = Reusability.derive
  implicit val rootModelReuse: Reusability[RootModel]                                         = Reusability.derive
  implicit val focusedReuse: Reusability[Focused]                                             = Reusability.derive
  implicit def idListReuse[Id, A: Reusability]: Reusability[KeyedIndexedList[Id, A]]          =
    Reusability.by(_.toList)
  implicit val ephemerisKeyReuse: Reusability[EphemerisKey]                                   = Reusability.derive
  implicit val siderealTargetReuse: Reusability[SiderealTarget]                               = Reusability.derive
  implicit val nonsiderealTargetReuse: Reusability[NonsiderealTarget]                         = Reusability.derive
  implicit val targetReuse: Reusability[Target]                                               = Reusability.derive
  implicit def scienceTargetReuse[T]( // format: off
    implicit @unused ev: Reusability[T] // We need the implicit parameter, but compiler complains it's unused.
  ): Reusability[ScienceTarget[T]] = Reusability.derive // format: on
  implicit val scienceTargetsReuse: Reusability[TreeSeqMap[Target.Id, ScienceTarget[Target]]] =
    Reusability.never // It's faster to rerender than to compare the whole thing.
  implicit val targetEnvReuse: Reusability[TargetEnv]                                          = Reusability.derive
  implicit val airMassRangeReuse: Reusability[AirMassRange]                                    = Reusability.derive
  implicit val hourAngleRangeReuse: Reusability[HourAngleRange]                                = Reusability.derive
  implicit val elevationRangeReuse: Reusability[ElevationRange]                                = Reusability.derive
  implicit val constraintsSummaryReuse: Reusability[ConstraintsSummary]                        = Reusability.byEq
  implicit val constraintsSetReuse: Reusability[ConstraintSet]                                 = Reusability.derive
  implicit val constraintGroupReuse: Reusability[ConstraintGroup]                              = Reusability.derive
  implicit val proposalDetailsReuse: Reusability[ProposalDetails]                              = Reusability.byEq
  implicit val partnerSplitReuse: Reusability[PartnerSplit]                                    = Reusability.derive
  implicit val obsSummaryReuse: Reusability[ObsSummary]                                        = Reusability.byEq
  implicit val obsSummaryWithConstraintsReuse: Reusability[ObsSummaryWithConstraints]          =
    Reusability.derive
  implicit val obsSummaryWithTargetsAndConstraintsReuse
    : Reusability[ObsSummaryWithTargetsAndConstraints] = Reusability.derive
  implicit def undoStacksReuse[F[_], M]: Reusability[UndoStacks[F, M]]                         =
    Reusability.by(s => (s.undo.length, s.redo.length, s.working))
  implicit def undoContextReuse[F[_], G[_], M: Reusability]: Reusability[UndoContext[F, G, M]] =
    Reusability.by(x => (x.model, x.stacks))

  implicit def undoSetterReuse[F[_], G[_], M: Reusability]: Reusability[UndoSetter[F, G, M]] =
    Reusability.by(_.model)

  implicit def undoStacksMapReuse[F[_], K, M]: Reusability[Map[K, UndoStacks[F, M]]] =
    Reusability.by[Map[K, UndoStacks[F, M]], Int](_.size) && Reusability[Map[K, UndoStacks[F, M]]](
      (a, b) =>
        a.forall { case (k, stacksA) =>
          b.get(k).exists(stacksB => undoStacksReuse.test(stacksA, stacksB))
        }
    )
  implicit def modelUndoStacksReuse[F[_]]: Reusability[ModelUndoStacks[F]]           = Reusability.derive
  // Move to lucuma-ui
  implicit val semesterReuse: Reusability[Semester]                                  = Reusability.derive
  implicit val cssReuse: Reusability[Css]                                            = Reusability.by(_.htmlClass)
}
