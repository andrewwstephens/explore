package explore.common

import clue.GraphQLOperation
import clue.annotation.GraphQL
import explore.schemas.ObservationDB
// gql: import explore.model.reusability._
// gql: import lucuma.ui.reusability._

object SequenceStepsGQL {

  @GraphQL
  trait SequenceSteps extends GraphQLOperation[ObservationDB] {
    val document = """
      query($first: Int = 2147483647) {
        observations(programId:"p-2", first: $first) {
          nodes {
            config {
              ... on GmosSouthConfig {
                instrument
                plannedTime {
                  total {
                    milliseconds
                  }
                }
                science {
                  atoms {
                    steps {
                      step {
                        stepType
                        instrumentConfig {
                          exposure {
                            milliseconds
                          }
                        }
                        stepConfig {
                          ... on Science {
                            offset {
                              p {
                                microarcseconds
                              }
                              q {
                                microarcseconds
                              }
                            }
                          }
                        }
                      }
                      time {
                        total {
                          milliseconds
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }  
    """
  }
}
