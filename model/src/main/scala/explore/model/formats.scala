// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package explore.model

import java.text.NumberFormat
import java.util.Locale

import explore.optics._
import lucuma.core.math.Parallax
import lucuma.core.math.ProperVelocity.AngularVelocityComponent
import lucuma.core.math._
import lucuma.core.math.units._
import lucuma.core.optics.Format
import lucuma.core.syntax.string._

trait formats {
  val pxFormat: Format[String, Parallax] =
    Format(_.parseBigDecimalOption.map(l => Parallax.fromMicroarcseconds((l * 1000L).toLong)),
           _.mas.to[BigDecimal, MilliArcSecond].value.toString
    )

  private def angularVelocityFormat[A](
    reverseGet: BigDecimal => AngularVelocityComponent[A]
  ): Format[String, AngularVelocityComponent[A]] =
    Format(_.parseBigDecimalOption.map(reverseGet),
           _.masy.to[BigDecimal, MilliArcSecondPerYear].value.toString
    )

  val pvRAFormat: Format[String, ProperVelocity.RA] = angularVelocityFormat(
    ProperVelocity.RA.milliarcsecondsPerYear.reverseGet
  )

  val pvDecFormat: Format[String, ProperVelocity.Dec] = angularVelocityFormat(
    ProperVelocity.Dec.milliarcsecondsPerYear.reverseGet
  )

  def formatterZ(dig: Int) = {
    val fmt = NumberFormat.getInstance(Locale.US)
    fmt.setGroupingUsed(false)
    fmt.setMaximumFractionDigits(scala.math.max(3, dig + 3))
    fmt
  }

  val formatterRV = {
    val fmt = NumberFormat.getInstance(Locale.US)
    fmt.setGroupingUsed(false)
    fmt.setMaximumFractionDigits(3)
    fmt
  }

  val formatterCZ = {
    val fmt = NumberFormat.getInstance(Locale.US)
    fmt.setGroupingUsed(false)
    fmt.setMaximumFractionDigits(3)
    fmt
  }

  val formatBigDecimalZ: Format[String, BigDecimal] =
    Format(_.parseBigDecimalOption, z => formatterZ(z.scale - z.precision).format(z))

  val formatBigDecimalCZ: Format[String, BigDecimal] =
    Format(_.parseBigDecimalOption, formatterCZ.format)

  val formatBigDecimalRV: Format[String, BigDecimal] =
    Format(_.parseBigDecimalOption, rv => formatterRV.format(rv))

  val formatRV: Format[String, RadialVelocity] =
    formatBigDecimalRV.composePrism(fromKilometersPerSecondRV)

  val formatZ: Format[String, Redshift] =
    formatBigDecimalZ.composeIso(redshiftBigDecimalISO)

  val formatCZ: Format[String, ApparentRadialVelocity] =
    formatBigDecimalCZ.composeIso(fromKilometersPerSecondCZ)
}

object formats extends formats
