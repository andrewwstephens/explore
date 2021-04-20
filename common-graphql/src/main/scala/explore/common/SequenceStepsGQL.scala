package explore.common

import clue.GraphQLOperation
import clue.annotation.GraphQL
import explore.schemas.ObservationDB
// gql: import explore.model.reusability._
// gql: import io.circe.refined._
// gql: import lucuma.ui.reusability._

object SequenceStepsGQL {

  @GraphQL
  trait SequenceSteps extends GraphQLOperation[ObservationDB] {
    val document = """
      query {
        observations(programId:"p-2", first:1000) {
          nodes {
            id
            name
            config {
              __typename
              ... on GmosNorthConfig {
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
                          __typename
                          ... on Gcal {
                            continuum
                            arcs
                            filter
                            diffuser
                            shutter
                          }
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
                          __typename
                          ... on Gcal {
                            continuum
                            arcs
                            filter
                            diffuser
                            shutter
                          }                    
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
