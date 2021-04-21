package explore.common

import java.time.Duration
import java.time.temporal.ChronoUnit

trait Durationµs {
  val microseconds: Long

  lazy val duration: Duration = Duration.of(microseconds, ChronoUnit.MICROS)
}
