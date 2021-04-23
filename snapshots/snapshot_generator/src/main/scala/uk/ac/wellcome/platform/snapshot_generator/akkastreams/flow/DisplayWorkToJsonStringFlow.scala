package uk.ac.wellcome.platform.snapshot_generator.akkastreams.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.display.json.DisplayJsonUtil
import uk.ac.wellcome.display.models.Implicits._

object DisplayWorkToJsonStringFlow extends Logging {
  def apply(): Flow[DisplayWork, String, NotUsed] =
    Flow[DisplayWork]
      .map {
        case work: DisplayWork => DisplayJsonUtil.toJson(work)
        case obj =>
          throw new IllegalArgumentException(
            s"Unrecognised object: ${obj.getClass}")
      }
}
