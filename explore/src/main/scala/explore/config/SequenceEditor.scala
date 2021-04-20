package explore.config

import explore.AppCtx
import explore.common.SequenceStepsGQL._
import explore.implicits._
import explore.schemas.ObservationDB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import react.common._
import explore.components.graphql.LiveQueryRender

final case class SequenceEditor() extends ReactProps[SequenceEditor](SequenceEditor.component)

object SequenceEditor {
  type Props = SequenceEditor

  private def renderFn(config: SequenceSteps.Data.Observations.Nodes.Config): VdomNode =
    SequenceTable(config)

  val component =
    ScalaComponent
      .builder[Props]
      .render(_ =>
        AppCtx.using { implicit ctx =>
          LiveQueryRender[ObservationDB,
                          SequenceSteps.Data,
                          SequenceSteps.Data.Observations.Nodes.Config
          ](SequenceSteps.query(), _.observations.nodes.head.config.get, List.empty)(
            (renderFn _).reusable
          )
        }
      )
      .build
}
