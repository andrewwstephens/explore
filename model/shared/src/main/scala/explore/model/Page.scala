// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.model

import cats.Eq
import cats.syntax.all._
import lucuma.core.model.Observation
import lucuma.core.model.Target
import monocle.Iso

sealed trait Page extends Product with Serializable

object Page {
  case object HomePage                                                           extends Page
  final case object ProposalPage                                                 extends Page
  final case object ObservationsBasePage                                         extends Page
  final case class ObsPage(obsId: Observation.Id)                                extends Page
  final case class ObsTargetPage(obsId: Observation.Id, targetId: Target.Id)     extends Page
  final case object TargetsBasePage                                              extends Page
  final case class TargetsObsPage(obsId: Observation.Id)                         extends Page
  final case class TargetPage(targetId: Target.Id)                               extends Page
  final case class TargetWithObsPage(obsId: Observation.Id, targetId: Target.Id) extends Page
  case object ConfigurationsPage                                                 extends Page
  final case object ConstraintsBasePage                                          extends Page
  final case class ConstraintsObsPage(obsId: Observation.Id)                     extends Page

  implicit val eqPage: Eq[Page] = Eq.instance {
    case (HomePage, HomePage)                                   => true
    case (ProposalPage, ProposalPage)                           => true
    case (ObservationsBasePage, ObservationsBasePage)           => true
    case (ObsPage(a), ObsPage(b))                               => a === b
    case (ObsTargetPage(o1, t1), ObsTargetPage(o2, t2))         => o1 === o2 && t1 === t2
    case (TargetsBasePage, TargetsBasePage)                     => true
    case (TargetsObsPage(a), TargetsObsPage(b))                 => a === b
    case (TargetPage(a), TargetPage(b))                         => a === b
    case (TargetWithObsPage(o1, t1), TargetWithObsPage(o2, t2)) => o1 === o2 && t1 === t2
    case (ConfigurationsPage, ConfigurationsPage)               => true
    case (ConstraintsBasePage, ConstraintsBasePage)             => true
    case (ConstraintsObsPage(a), ConstraintsObsPage(b))         => a === b
    case _                                                      => false
  }

  object ObsPage {
    final val obsId: Iso[Observation.Id, ObsPage] =
      Iso[Observation.Id, ObsPage](ObsPage.apply)(_.obsId)
  }

  object ObsTargetPage {
    final val iso: Iso[(Observation.Id, Target.Id), ObsTargetPage] =
      Iso[(Observation.Id, Target.Id), ObsTargetPage](t => ObsTargetPage(t._1, t._2))(p =>
        (p.obsId, p.targetId)
      )
  }

  object TargetsObsPage {
    final val obsId: Iso[Observation.Id, TargetsObsPage] =
      Iso[Observation.Id, TargetsObsPage](TargetsObsPage.apply)(_.obsId)
  }

  object TargetPage {
    final val targetId: Iso[Target.Id, TargetPage] =
      Iso[Target.Id, TargetPage](TargetPage.apply)(_.targetId)
  }

  object TargetWithObsPage {
    final val iso: Iso[(Observation.Id, Target.Id), TargetWithObsPage] =
      Iso[(Observation.Id, Target.Id), TargetWithObsPage](t => TargetWithObsPage(t._1, t._2))(p =>
        (p.obsId, p.targetId)
      )
  }

  object ConstraintsObsPage {
    final val obsId: Iso[Observation.Id, ConstraintsObsPage] =
      Iso[Observation.Id, ConstraintsObsPage](ConstraintsObsPage.apply)(_.obsId)
  }
}
