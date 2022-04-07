// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package queries.common

import clue.GraphQLOperation
import clue.annotation.GraphQL
import queries.schemas.ITC
// gql: import lucuma.ui.reusability._

object ITCQueriesGQL {

  @GraphQL
  trait SpectroscopyITCQuery extends GraphQLOperation[ITC] {
    val document =
      """
      query($input: SpectroscopyModeInput) {
        spectroscopy(input: $input) {
          results {
            mode {
              instrument
            }
            itc {
              ... on ItcSuccess {
                exposures
                exposureTime {
                  microseconds
                }
              }
              ... on ItcError {
                msg
              }
            }
          }
        }
      }
    """
  }

}