// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.observationtree

import cats.data.NonEmptyList
import cats.effect.IO
import clue.GraphQLOperation
import clue.macros.GraphQL
import explore.AppCtx
import explore.GraphQLSchemas._
import explore.components.graphql.LiveQueryRenderMod
import explore.data.KeyedIndexedList
import explore.implicits._
import explore.model.{ ConstraintsSummary, ObsSummary }
import explore.model.reusability._
import japgolly.scalajs.react.Reusability
import japgolly.scalajs.react.vdom.html_<^._
import lucuma.core.model.ConstraintSet
import lucuma.core.model.Observation
import lucuma.ui.reusability._
import monocle.Getter
import monocle.macros.Lenses

object ConstraintSetObsQueries {

  type ConstraintSetList = KeyedIndexedList[ConstraintSet.Id, ConstraintsSummary]
  type ObsList           = KeyedIndexedList[Observation.Id, ObsSummary]

  @Lenses
  case class ConstraintSetsWithObs(constraintSets: ConstraintSetList, obs: ObsList)

  @GraphQL(debug = false)
  object ConstraintSetsObsQuery extends GraphQLOperation[ObservationDB] {
    val document = s"""
      query {
        constraintSets(programId: "p-2", first: ${Int.MaxValue}) {
          nodes {
            id
            name
            imageQuality
            cloudExtinction
            skyBackground
            waterVapor
          }
        }

        observations(programId: "p-2", first: ${Int.MaxValue}) {
          nodes {
            id
            name
            observationTarget {
              type: __typename
              ... on Target {
                target_id: id
              }
              ... on Asterism {
                asterism_id: id
              }
            }
            constraintSet {
              id
              name
              imageQuality
              cloudExtinction
              skyBackground
              waterVapor
            }            
          }
        }    
      }
    """

    object Data {
      object ConstraintSets {
        type Nodes = ConstraintsSummary
      }

      object Observations {
        type Nodes = ObsSummary
      }

      val asConstraintSetsWithObs: Getter[Data, ConstraintSetsWithObs] = data => {
        ConstraintSetsWithObs(
          KeyedIndexedList.fromList(data.constraintSets.nodes, ConstraintsSummary.id.get),
          KeyedIndexedList.fromList(data.observations.nodes, ObsSummary.id.get)
        )
      }
    }
  }

  @GraphQL
  object CostraintSetEditSubscription extends GraphQLOperation[ObservationDB] {
    val document = """
      subscription {
        constraintSetEdit(programId: "p-2") {
          id
        }
      }
    """
  }

  @GraphQL
  object RemoveConstraintSet extends GraphQLOperation[ObservationDB] {
    val document = """
      mutation($constraintSetId: ConstraintSetId!) {
        deleteConstraintSet(constraintSetId: $constraintSetId) {
          id
        }
      }
    """
  }

  @GraphQL
  object ShareConstraintSetWithObs extends GraphQLOperation[ObservationDB] {
    val document: String = """
      mutation($constraintSetId: ConstraintSetId!, $obsId: ObservationId!) {
        shareConstraintSetWithObservations(
          input: { constraintSetId: $constraintSetId, observationIds: [$obsId] }
        ) {
          id
        }
      }
    """
  }

  @GraphQL
  object UnshareConstraintSetWithObs extends GraphQLOperation[ObservationDB] {
    val document: String = """
      mutation($constraintSetId: ConstraintSetId!, $obsId: ObservationId!) {
        unshareConstraintSetWithObservations(
          input: { constraintSetId: $constraintSetId, observationIds: [$obsId] }
        ) {
          id
        }
      }
    """
  }

  @GraphQL
  object UnassignConstraintSet extends GraphQLOperation[ObservationDB] {
    val document = """
      mutation($obsId: ObservationId!) {
        unsetConstraintSet(observationId: $obsId) {
          id
        }
      }
    """
  }

  implicit val constraintSetWithObsReusability: Reusability[ConstraintSetsWithObs] =
    Reusability.derive

  type LiveQueryRenderer =
    (
      View[ConstraintSetsWithObs] => VdomNode
    ) => LiveQueryRenderMod[ObservationDB, ConstraintSetsObsQuery.Data, ConstraintSetsWithObs]

  val ConstraintSetObsLiveQuery: LiveQueryRenderer =
    render =>
      AppCtx.runWithCtx { implicit appCtx =>
        LiveQueryRenderMod[ObservationDB, ConstraintSetsObsQuery.Data, ConstraintSetsWithObs](
          ConstraintSetsObsQuery.query(),
          ConstraintSetsObsQuery.Data.asConstraintSetsWithObs.get,
          NonEmptyList.of(
            CostraintSetEditSubscription.subscribe[IO](),
            ObsQueries.ProgramObservationsEditSubscription.subscribe[IO]()
          )
        )(render)
      }

}
