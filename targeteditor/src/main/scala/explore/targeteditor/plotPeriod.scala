// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.targeteditor

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

import lucuma.core.enum.Site
import lucuma.core.model.Semester
import lucuma.core.math.skycalc.solver.Samples
import lucuma.core.math.Interval
import lucuma.core.math.Coordinates
import lucuma.core.model.TwilightBoundedNight
import lucuma.core.enum.TwilightType
import lucuma.core.math.skycalc.SkyCalcResults
import scala.collection.immutable.TreeMap
import cats.syntax.all._
import io.chrisdavenport.cats.time._
import java.time.LocalTime
import cats.Eval
import lucuma.core.math.skycalc.solver.ElevationSolver
import lucuma.core.math.Declination

sealed trait PlotPeriod[R] {
  def interval(site: Site): Interval

  val dateTimeFomat: String

  protected val rate: Duration

  protected def coordSamples(
    interval:         Interval,
    coordsForInstant: Instant => Coordinates
  ): Samples[Coordinates] =
    Samples.atFixedRate(interval, rate)(coordsForInstant)

  def samples(
    site:             Site,
    coordsForInstant: Instant => Coordinates
  ): Samples[R]
}

object PlotPeriod {
  final case class NightPlot(date: LocalDate) extends PlotPeriod[SkyCalcResults] {
    override def interval(site: Site): Interval =
      TwilightBoundedNight
        .fromTwilightTypeAndSiteAndLocalDateUnsafe(TwilightType.Official, site, date)
        .interval

    override val dateTimeFomat: String = "%H:%M"

    override protected val rate: Duration = Duration.ofMinutes(1)

    override def samples(
      site:             Site,
      coordsForInstant: Instant => Coordinates
    ): Samples[SkyCalcResults] =
      coordSamples(interval(site), coordsForInstant).toSkyCalResultsAt(site.place)
  }

  final case class SemesterPlot(semester: Semester) extends PlotPeriod[Duration] {
    override def interval(site: Site): Interval =
      Interval.unsafe(semester.start.atSite(site).toInstant, semester.end.atSite(site).toInstant)

    override val dateTimeFomat: String = "%Y-%m-%d"

    override protected val rate: Duration = Duration.ofMinutes(10)

    private val MinTargetElevation = Declination.Zero

    override def samples(
      site:             Site,
      coordsForInstant: Instant => Coordinates
    ): Samples[Duration] = {
      val results       = Samples
        .atFixedRate(Interval.unsafe(semester.start.atSite(site).toInstant,
                                     semester.end.atSite(site).toInstant
                     ),
                     rate
        )(coordsForInstant)
        .toSkyCalResultsAt(site.place)
      val targetVisible = ElevationSolver(MinTargetElevation, Declination.Max).solve(results) _
      Samples.fromMap(
        Iterator
          .iterate(semester.start.localDate)(_.plusDays(1))
          .takeWhile(_ < semester.end.localDate)
          .map { date =>
            val instant = date.atTime(LocalTime.MIDNIGHT).atZone(site.timezone).toInstant
            instant -> Eval.later(
              targetVisible(
                TwilightBoundedNight
                  .fromTwilightTypeAndSiteAndLocalDateUnsafe(TwilightType.Nautical, site, date)
                  .interval
              ).duration
            )
          }
          .to(TreeMap)
      )
    }
  }
}
