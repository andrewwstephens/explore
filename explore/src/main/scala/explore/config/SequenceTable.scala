package explore.config

import explore.common.SequenceStepsGQL._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import react.common._

final case class SequenceTable(config: SequenceSteps.Data.Observations.Nodes.Config)
    extends ReactProps[SequenceTable](SequenceTable.component)

object SequenceTable {
  type Props = SequenceTable

  val component =
    ScalaComponent
      .builder[Props]
      .render(_ => <.div)
      .build
}
