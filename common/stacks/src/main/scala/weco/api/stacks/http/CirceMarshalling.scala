package weco.api.stacks.http

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.Future

/** This class gives us helpers for using Circe to marshall/unmarshall JSON
  * using our Circe decoders and encoders.
  *
  * This is helpful for a few reasons:
  *
  *   1.  The default [Un]marshallers aren't aware of all of Circe's features,
  *       e.g. the @JsonKey annotation.
  *   2.  It means that any method doing akka-http marshalling can just require
  *       an implicit Decoder[T] or Encoder[T], which is a more common interface
  *       in our codebase.
  *
  */
object CirceMarshalling {
  def fromDecoder[T: Decoder]: Unmarshaller[HttpEntity, T] =
    Unmarshaller.stringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`)
      .flatMap { _ => _ => json =>
        Future.fromTry(fromJson[T](json))
      }

  def fromEncoder[T: Encoder]: ToEntityMarshaller[T] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { t =>
      HttpEntity(ContentTypes.`application/json`, t.asJson.noSpaces)
    }
}
