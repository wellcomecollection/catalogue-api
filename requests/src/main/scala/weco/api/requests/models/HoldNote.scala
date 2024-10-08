package weco.api.requests.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object HoldNote {
  val pickupDateLabel = "Requested for"
  val pickupDateFormat =
    "yyyy-MM-dd" // Format as per https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html

  private lazy val formatter = DateTimeFormatter.ofPattern(pickupDateFormat)
  def createPickupDate(pickupDate: LocalDate): String =
    s"$pickupDateLabel: ${formatter.format(pickupDate)}"

  private val pickupDateNotePattern = s"$pickupDateLabel: (.+)".r
  def parsePickupDate(note: String): Option[LocalDate] = note match {
    // Parse any date we can here (rather than reusing the formatter) to make changing the format easier in future
    case pickupDateNotePattern(dateString) =>
      Try(LocalDate.parse(dateString)).toOption
    case _ => None
  }
}
