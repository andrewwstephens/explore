// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.itc

import cats.data.NonEmptyList
import cats.syntax.all._
import crystal.Pot
import explore.components.ui.ExploreStyles
import explore.highcharts.*
import explore.implicits._
import explore.model.enums.ItcChartType
import explore.model.itc.ItcChart
import explore.model.itc.ItcSeries
import explore.model.itc.YAxis
import explore.syntax.ui.*
import explore.syntax.ui.given
import explore.utils.*
import gpp.highcharts.highchartsStrings.line
import gpp.highcharts.mod._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import lucuma.core.util.Enumerated
import lucuma.ui.syntax.all.*
import lucuma.ui.syntax.all.given
import react.common.ReactFnProps
import react.highcharts.Chart
import react.resizeDetector.hooks._
import react.semanticui.collections.form.Form
import react.semanticui.elements.button.Button
import react.semanticui.elements.button.ButtonGroup
import react.semanticui.elements.loader.Loader
import react.semanticui.sizes._

import scala.collection.immutable.HashSet
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

case class ItcSpectroscopyPlot(
  loading:   PlotLoading,
  charts:    Pot[NonEmptyList[ItcChart]],
  error:     Option[String],
  chartType: ItcChartType,
  details:   PlotDetails // Used only as part of the key
) extends ReactFnProps[ItcSpectroscopyPlot](ItcSpectroscopyPlot.component)

object ItcSpectroscopyPlot {
  type Props = ItcSpectroscopyPlot

  def chartOptions(chart: ItcChart, loading: PlotLoading, height: Double) = {
    val yAxis            = chart.series.foldLeft(YAxis.Empty)(_ ∪ _.yAxis)
    val title            = chart.chartType match
      case ItcChartType.SignalChart => "e⁻ per exposure per spectral pixel"
      case ItcChartType.S2NChart    => "S/N per spectral pixel"
    val (min, max, tick) = yAxis.ticks(10)
    val yAxes            = YAxisOptions()
      .setTitle(YAxisTitleOptions().setText(title))
      .setAllowDecimals(false)
      .setTickInterval(tick)
      .setMin(min)
      .setMax(max)
      .setMinorTickInterval(tick / 3)
      .setLineWidth(1)
      .setLabels(YAxisLabelsOptions().setFormat("{value}"))

    Options()
      .setChart(
        ChartOptions()
          .setHeight(height)
          .setStyledMode(true)
          .setAlignTicks(false)
          .clazz(
            ExploreStyles.ItcPlotChart |+|
              ExploreStyles.ItcPlotLoading.when_(loading.boolValue)
          )
          .setZoomType(OptionsZoomTypeValue.xy)
          .setPanning(ChartPanningOptions().setEnabled(true))
          .setPanKey(OptionsPanKeyValue.shift)
          .setAnimation(false)
          // Will be used in the future to persist the soom
          // .selectionCB(s => Callback.log(s"selection ${s.xAxis(0).min}"))
      )
      .setTitle(TitleOptions().setTextUndefined)
      .setCredits(CreditsOptions().setEnabled(false))
      .setLegend(LegendOptions().setMargin(0))
      .setXAxis(
        XAxisOptions()
          .setType(AxisTypeValue.linear)
          .setTitle(XAxisTitleOptions().setText("Wavelength nm"))
      )
      .setYAxis(List(yAxes).toJSArray)
      .setPlotOptions(
        PlotOptions()
          .setSeries(
            PlotSeriesOptions()
              .setLineWidth(4)
              .setMarker(PointMarkerOptionsObject().setEnabled(false).setRadius(0))
              .setStates(
                SeriesStatesOptionsObject()
                  .setHover(SeriesStatesHoverOptionsObject().setEnabled(false))
              )
          )
      )
      .setSeries(
        chart.series
          .map(series =>
            SeriesLineOptions((), (), line)
              .setName(series.title)
              .setYAxis(0)
              .setData(series.data.map(p => (p(0), p(1)): Chart.Data).toJSArray)
              .setLineWidth(1)
          )
          .map(_.asInstanceOf[SeriesOptionsType])
          .toJSArray
      )
  }

  def emptyChartOptions(height: Double) = {
    val yAxis = YAxisOptions()
      .setAllowDecimals(false)
      .setMin(0)
      .setMax(100)
      .setTickInterval(10)

    Options()
      .setChart(
        ChartOptions()
          .setHeight(height)
          .setStyledMode(true)
          .setAlignTicks(false)
          .clazz(
            ExploreStyles.ItcPlotChart |+| ExploreStyles.ItcPlotLoading
          )
          // Will be used in the future to persist the soom
          // .selectionCB(s => Callback.log(s"selection ${s.xAxis(0).min}"))
      )
      .setTitle(TitleOptions().setTextUndefined)
      .setCredits(CreditsOptions().setEnabled(false))
      .setXAxis(
        XAxisOptions()
          .setType(AxisTypeValue.linear)
      )
      .setYAxis(List(yAxis).toJSArray)
      .setPlotOptions(
        PlotOptions()
          .setSeries(
            PlotSeriesOptions()
              .setLineWidth(4)
              .setMarker(PointMarkerOptionsObject().setEnabled(false).setRadius(0))
              .setStates(
                SeriesStatesOptionsObject()
                  .setHover(SeriesStatesHoverOptionsObject().setEnabled(false))
              )
          )
      )
  }

  val component = ScalaFnComponent
    .withHooks[Props]
    .useResizeDetector()
    .render { (props, resize) =>
      val loading = props.charts.isPending || props.loading.boolValue

      val series: List[ItcChart] =
        props.charts.toOption.filterNot(_ => loading).map(_.toList).orEmpty

      val height          = resize.height.getOrElse(1).toDouble
      val itcChartOptions = series.map { chart =>
        chart.chartType -> chartOptions(chart, props.loading, height)
      }.toMap

      <.div(
        ExploreStyles.ItcPlotBody,
        itcChartOptions
          .get(props.chartType)
          .map { opt =>
            Chart(opt,
                  onCreate = c =>
                    c.showLoadingCB.when_(loading) *>
                      props.error
                        .map(e => c.showLoadingCB(e).unless_(loading))
                        .orEmpty
            )
              .withKey(s"$props-$resize")
              .when(resize.height.isDefined)
          }
          .getOrElse(
            Chart(emptyChartOptions(height),
                  onCreate = c =>
                    c.showLoadingCB.when_(loading) *>
                      props.error
                        .map(e => c.showLoadingCB(e).unless_(loading))
                        .orEmpty
            )
              .withKey(s"$props-$resize")
              .when(resize.height.isDefined)
          )
      ).withRef(resize.ref)
    }
}