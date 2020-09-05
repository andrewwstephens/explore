// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.targeteditor

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import scala.scalajs.js

import explore.model.reusability._
import gpp.highcharts.highchartsStrings.line
import gpp.highcharts.mod.XAxisLabelsOptions
import gpp.highcharts.mod._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import lucuma.core.enum.Site
import lucuma.core.math.Coordinates
import lucuma.ui.reusability._
import react.common._
import react.highcharts.Chart

import js.JSConverters._
import lucuma.core.model.Semester

final case class SemesterPlot(
  site:     Site,
  coords:   Coordinates,
  semester: Semester,
  zoneId:   ZoneId,
  height:   Int
) extends ReactProps[SemesterPlot](SemesterPlot.component)

object SemesterPlot {
  type Props = SemesterPlot

  implicit private val propsReuse: Reusability[Props] = Reusability.derive

  private val MillisPerHour: Double = 60 * 60 * 1000
  private val MillisPerDay: Double  = MillisPerHour * 24

  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  class Backend() {
    def render(props: Props) = {
      val series =
        SemesterPlotCalc(props.semester)
          .samples(props.site, _ => props.coords)
          .toMap
          .view
          .mapValues(_.value.toMillis)
          .toList
          .map {
            case (instant, visibility) =>
              PointOptionsObject()
                .setX(instant.toEpochMilli.toDouble)
                // Trick to leave small values out of the plot
                .setY(if (visibility > 0.1) visibility / MillisPerHour else -1): Chart.Data
          }

      def timeFormat(value: Double): String =
        ZonedDateTime
          .ofInstant(Instant.ofEpochMilli(value.toLong), props.zoneId)
          .format(dateTimeFormatter)

      // val timeZone: String =
      //   props.zoneId match {
      //     case ZoneOffset.UTC => "UTC"
      //     case other          => other.getId
      //   }

      val tickFormatter: AxisLabelsFormatterCallbackFunction =
        (
          labelValue: AxisLabelsFormatterContextObject[Double],
          _:          AxisLabelsFormatterContextObject[String]
        ) => timeFormat(labelValue.value)

      // val tooltipFormatter: TooltipFormatterCallbackFunction = {
      //   (ctx: TooltipFormatterContextObject, _: Tooltip) =>
      //     val time  = timeFormat(ctx.x)
      //     val value = ctx.series.index match {
      //       case 2 => // Sky Brightness
      //         "%0.2f".format(ctx.y)
      //       case _ => formatAngle(ctx.y)
      //     }
      //     s"<strong>$time ($timeZone)</strong><br/>${ctx.series.name}: $value"
      // }

      val options = Options()
        .setChart(ChartOptions().setHeight(props.height).setStyledMode(true).setAlignTicks(false))
        .setTitle(
          TitleOptions().setText(
            s"Semester ${props.semester.format}"
          )
        )
        .setCredits(CreditsOptions().setEnabled(false))
        // .setTooltip(TooltipOptions().setFormatter(tooltipFormatter))
        .setXAxis(
          XAxisOptions()
            .setType(AxisTypeValue.datetime)
            .setLabels(XAxisLabelsOptions().setFormatter(tickFormatter))
            .setTickInterval(MillisPerDay * 10)
            .setMinorTickInterval(MillisPerDay * 5)
        )
        .setYAxis(
          List(
            YAxisOptions()
              .setTitle(YAxisTitleOptions().setText("Hours"))
              .setAllowDecimals(false)
              .setMin(0)
              .setMax(15)
              .setTickInterval(1)
              .setMinorTickInterval(0.5)
              .setLabels(YAxisLabelsOptions().setFormat("{value}"))
          ).toJSArray
        )
        .setPlotOptions(
          PlotOptions()
            .setSeries(
              PlotSeriesOptions()
                .setLineWidth(4)
                .setMarker(PointMarkerOptionsObject().setEnabled(false))
                .setStates(
                  SeriesStatesOptionsObject()
                    .setHover(SeriesStatesHoverOptionsObject().setEnabled(false))
                )
            )
        )
        .setSeries(
          List(
            SeriesLineOptions(line)
              .setName("Visibility")
              .setYAxis(0)
              .setData(series.toJSArray)
          )
            .map(_.asInstanceOf[SeriesOptionsType])
            .toJSArray
        )

      <.span(
        Chart(options).withKey(props.toString)
      )
    }
  }

  val component =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .configure(Reusability.shouldComponentUpdate)
      .build
}
