package explore.config

// import explore.common.SequenceStepsGQL._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import react.common._
import explore.common.SequenceStepsGQL.SequenceSteps.Data.Observations.Nodes.Config
import reactST.reactTable.TableMaker

import scalajs.js.JSConverters._

final case class SequenceTable(config: Config)
    extends ReactProps[SequenceTable](SequenceTable.component)

object SequenceTable {
  type Props = SequenceTable

  val component =
    ScalaComponent
      .builder[Props]
      .render_P { props =>
        val tableMaker = TableMaker[Config.GmosSouthConfig.Science.Atoms.Steps]
        // import tableMaker.syntax._

        val columns = tableMaker.columnArray(
          tableMaker
            .accessorColumn("stepType", _.step.stepType.toString)
            .setHeader("Step Type"),
          tableMaker
            .accessorColumn("time", _.time.toString)
            .setHeader("Time")
        )

        val options = tableMaker
          .options(rowIdFn = _.time.toString, columns = columns)
        // .setInitialStateFull(tableState)

        <.div(
          "HELLO!",
          props.config match {
            case Config.GmosSouthConfig(_, _, science) =>
              tableMaker.makeTable(
                options = options,
                data = science.atoms.flatMap(_.steps).toJSArray,
                headerCellFn = Some(c =>
                  TableMaker
                    .basicHeaderCellFn(Css.Empty)(c)
                ),
                tableClass = Css("ui very celled selectable striped compact table")
              )
            case _                                     => "North config!"
          }
        )
      }
      .build
}
