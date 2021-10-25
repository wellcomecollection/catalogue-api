package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Decoder
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.identifiers.IdState.Minted
import weco.catalogue.internal_model.languages.{Language, MarcLanguageCodeList}
import weco.catalogue.internal_model.locations.License
import weco.catalogue.internal_model.work._

import java.time.{Instant, LocalDate, LocalDateTime, Month, ZoneOffset}
import scala.util.Try

case class WorkAggregations(
  format: Option[Aggregation[Format]] = None,
  genresLabel: Option[Aggregation[Genre[Minted]]] = None,
  productionDates: Option[Aggregation[Period[Minted]]] = None,
  languages: Option[Aggregation[Language]] = None,
  subjectsLabel: Option[Aggregation[Subject[Minted]]] = None,
  contributorsAgentsLabel: Option[Aggregation[Contributor[Minted]]] = None,
  itemsLocationsLicense: Option[Aggregation[License]] = None,
  availabilities: Option[Aggregation[Availability]] = None
)

object WorkAggregations extends ElasticAggregations {

  def apply(searchResponse: SearchResponse): Option[WorkAggregations] = {
    val e4sAggregations = searchResponse.aggregations
    if (e4sAggregations.data.nonEmpty) {
      Some(
        WorkAggregations(
          format = e4sAggregations.decodeAgg[Format]("format"),
          genresLabel = e4sAggregations.decodeAgg[Genre[Minted]]("genres"),
          productionDates = e4sAggregations
            .decodeAgg[Period[Minted]]("productionDates"),
          languages = e4sAggregations.decodeAgg[Language]("languages"),
          subjectsLabel = e4sAggregations
            .decodeAgg[Subject[Minted]]("subjects"),
          // TODO decode only agents here once `contributors` is removed
          contributorsAgentsLabel = e4sAggregations
            .decodeAgg[Contributor[Minted]]("contributors"),
          itemsLocationsLicense = e4sAggregations.decodeAgg[License]("license"),
          availabilities =
            e4sAggregations.decodeAgg[Availability]("availabilities")
        )
      )
    } else {
      None
    }
  }

  // Elasticsearch encodes the date key as milliseconds since the epoch
  implicit val decodePeriodFromEpochMilli: Decoder[Period[IdState.Minted]] =
    Decoder.decodeLong.emap { epochMilli =>
      Try { Instant.ofEpochMilli(epochMilli) }
        .map { instant =>
          LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        }
        .map { date =>
          val range = InstantRange(
            from = LocalDate.of(date.getYear, Month.JANUARY, 1),
            to = LocalDate.of(date.getYear, Month.DECEMBER, 31),
            label = date.getYear.toString
          )

          Right(
            Period(
              label = range.label,
              range = range
            )
          )
        }
        .getOrElse { Left("Error decoding") }
    }

  implicit val decodeFormatFromId: Decoder[Format] =
    Decoder.decodeString.emap(Format.withNameEither(_).getMessage)

  implicit val decodeAvailabilityFromId: Decoder[Availability] =
    Decoder.decodeString.emap(Availability.withNameEither(_).getMessage)

  // Both the Calm and Sierra transformers use the MARC language code list
  // to populate the "languages" field, so we can use the ID (code) to
  // unambiguously identify a language.
  implicit val decodeLanguageFromCode: Decoder[Language] =
    Decoder.decodeString.emap { code =>
      MarcLanguageCodeList.fromCode(code) match {
        case Some(lang) => Right(lang)
        case None       => Left(s"couldn't find language for code $code")
      }
    }

  implicit val decodeSubjectFromLabel: Decoder[Subject[Minted]] =
    Decoder.decodeString.map { str =>
      Subject(label = str, concepts = Nil)
    }

  implicit val decodeContributorFromLabel: Decoder[Contributor[Minted]] =
    decodeAgentFromLabel.map(agent => Contributor(agent = agent, roles = Nil))

}
