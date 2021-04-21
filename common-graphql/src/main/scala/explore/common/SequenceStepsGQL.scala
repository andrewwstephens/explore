package explore.common

import clue.GraphQLOperation
import clue.annotation.GraphQL
import explore.schemas.ObservationDB
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
              instrument
              plannedTime {
                total {
                  microseconds
                }
              }              
              ... on GmosNorthConfig {
                science {
                  atoms {
                    steps {
                      stepType
                      instrumentConfig {
                        exposure {
                          microseconds
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
                      time {
                        total {
                          microseconds
                        }
                      }
                    }
                  }
                }
              }
              ... on GmosSouthConfig {
                science {
                  atoms {
                    steps {
                      stepType
                      instrumentConfig {
                        exposure {
                          microseconds
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
                      time {
                        total {
                          microseconds
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

    object Data {
      object Observations {
        object Nodes {
          object Config {
            object GmosNorthConfig {
              object Science {
                object Atoms {
                  object Steps {
                    object StepType
                    object InstrumentConfig {
                      trait Exposure extends Durationµs
                    }
                    object Time             {
                      trait Total extends Durationµs
                    }
                  }
                }
              }
            }

            object GmosSouthConfig {
              object Science {
                object Atoms {
                  object Steps {
                    object StepType
                    object InstrumentConfig {
                      trait Exposure extends Durationµs
                    }
                    object Time             {
                      trait Total extends Durationµs
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
}
