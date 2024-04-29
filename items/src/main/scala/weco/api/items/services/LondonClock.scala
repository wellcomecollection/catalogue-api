package weco.api.items.services

import java.time.{ZoneId, ZonedDateTime}

class LondonClock {
  def timeInLondon(): ZonedDateTime =
    ZonedDateTime.now.withZoneSameLocal(ZoneId.of("Europe/London"))
}
