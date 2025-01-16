package weco.api.search.rest

import java.time.LocalDate
import org.apache.pekko.http.scaladsl.server.{
  Directive,
  Directives,
  ValidationRejection
}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller
import com.github.tototoshi.csv.CSVParser
import io.circe.{Decoder, Json}
import weco.api.search.rest.MultipleWorksParams.{
  decodeCommaSeparated,
  stringListFilter
}
import weco.api.search.models.{
  ContributorsIdFilter,
  ContributorsLabelFilter,
  GenreIdFilter,
  GenreLabelFilter,
  LicenseFilter,
  SubjectIdFilter,
  SubjectLabelFilter
}

trait QueryParams

object CommonDecoders {
  implicit val licenseFilter: Decoder[LicenseFilter] =
    decodeCommaSeparated.emap(strs => Right(LicenseFilter(strs)))

  implicit val contributorsFilter: Decoder[ContributorsLabelFilter] =
    stringListFilter(ContributorsLabelFilter)

  implicit val genreFilter: Decoder[GenreLabelFilter] =
    stringListFilter(GenreLabelFilter)

  implicit val genreIdFilter: Decoder[GenreIdFilter] =
    stringListFilter(GenreIdFilter)

  implicit val subjectsFilter: Decoder[SubjectLabelFilter] =
    stringListFilter(SubjectLabelFilter)

  implicit val subjectsIdFilter: Decoder[SubjectIdFilter] =
    stringListFilter(SubjectIdFilter)

  implicit val contributorsConceptFilter: Decoder[ContributorsIdFilter] =
    stringListFilter(ContributorsIdFilter)

}

trait QueryParamsUtils extends Directives {

  implicit def unmarshaller[T](
    implicit decoder: Decoder[T]
  ): Unmarshaller[String, T] =
    Unmarshaller.strict[String, T] { str =>
      decoder.decodeJson(Json.fromString(str)) match {
        case Left(err)    => throw new IllegalArgumentException(err.message)
        case Right(value) => value
      }
    }

  implicit val decodeLocalDate: Decoder[LocalDate] =
    Decoder.decodeLocalDate
      .withErrorMessage("Invalid date encoding. Expected YYYY-MM-DD")
      .emap { date =>
        // This check is trying to avoid an Elasticsearch error.  We saw a query
        // for the date "+11860-01-01", which returned an exception from Elasticsearch:
        //
        //       "caused_by": {
        //         "caused_by": {
        //           "reason": "Failed to parse with all enclosed parsers",
        //           "type": "date_time_parse_exception"
        //         },
        //       "reason": "failed to parse date field [+11860-01-01] with format [strict_date_optional_time||epoch_millis]",
        //       "type": "illegal_argument_exception"
        //
        // This would cause a 500 Internal Server Error to be thrown, when what we should
        // be throwing is a 400 Bad Request.
        //
        // We could catch this when we get the response from Elasticsearch, but then it's
        // harder to map back to the user-supplied parameter where the value came from.
        // Since 9999 is the max date we use in the pipeline (as a synonym for "never"),
        // allowing users to request beyond this seems pointless.
        //
        // Catching it in the decoder lets us tell the user which parameter was causing
        // the issue.
        //
        // Note: this code will need rewriting sometime in the eleventh millenium to raise
        // the limit.  Hopefully Elasticsearch can handle five-digit years by then.
        Either.cond(
          date.getYear <= 9999,
          date,
          "year must be less than 9999"
        )
      }

  implicit val decodeInt: Decoder[Int] =
    Decoder.decodeInt.withErrorMessage("must be a valid Integer")

  def decodeCommaSeparated: Decoder[List[String]] =
    Decoder.decodeString.emap(
      str =>
        Right(
          CSVParser
            .parse(
              input = str,
              escapeChar = '\\',
              delimiter = ',',
              quoteChar = '"'
            )
            .getOrElse(List(str))
      )
    )

  def decodeOneOf[T](values: (String, T)*): Decoder[T] =
    Decoder.decodeString.emap { str =>
      values.toMap
        .get(str)
        .map(Right(_))
        .getOrElse(Left(invalidValuesMsg(List(str), values.map(_._1).toList)))
    }

  def decodeOneWithDefaultOf[T](default: T, values: (String, T)*): Decoder[T] =
    Decoder.decodeString.map { values.toMap.getOrElse(_, default) }

  def decodeOneOfCommaSeparated[T](values: (String, T)*): Decoder[List[T]] = {
    val mapping = values.toMap
    val validStrs = values.map(_._1).toList
    decodeCommaSeparated.emap { strs =>
      mapStringsToValues(strs, mapping).left
        .map { invalidStrs =>
          invalidValuesMsg(invalidStrs, validStrs)
        }
    }
  }

  case class IncludesAndExcludes(includes: List[String], excludes: List[String])

  def decodeIncludesAndExcludes(
    validStrs: Set[String]
  ): Decoder[IncludesAndExcludes] =
    decodeCommaSeparated
      .emap { strs =>
        // We get the invalid strings first, so we can report on them in the order
        // they were given if any aren't allowed.
        val invalidStrs = strs
          .filterNot { s =>
            validStrs.contains(s) || validStrs.contains(s.replaceAll("!", ""))
          }

        val (excludeStrs, includes) = strs.partition(_.startsWith("!"));
        val excludes = excludeStrs.map(s => s.replaceFirst("!", ""))

        Either.cond(
          invalidStrs.isEmpty,
          right = IncludesAndExcludes(includes, excludes),
          left = invalidValuesMsg(invalidStrs, validStrs.toList)
        )
      }

  def stringListFilter[T](applyFilter: Seq[String] => T): Decoder[T] =
    decodeCommaSeparated.emap(strs => Right(applyFilter(strs)))

  private def invalidValuesMsg(
    values: List[String],
    validValues: List[String]
  ): String = {
    val oneOfMsg =
      s"Please choose one of: [${validValues.mkString("'", "', '", "'")}]"
    values match {
      case value :: Nil => s"'$value' is not a valid value. $oneOfMsg"
      case _ =>
        s"${values.mkString("'", "', '", "'")} are not valid values. $oneOfMsg"
    }
  }

  def validated[T <: QueryParams](
    errors: List[String],
    params: T
  ): Directive[Tuple1[T]] =
    errors match {
      case Nil => provide(params)
      case errs =>
        reject(ValidationRejection(errs.mkString(", ")))
          .toDirective[Tuple1[T]]
    }

  private def mapStringsToValues[T](
    strs: List[String],
    mapping: Map[String, T]
  ): Either[List[String], List[T]] = {
    val results = strs.map { str =>
      mapping
        .get(str)
        .map(Right(_))
        .getOrElse(Left(str))
    }
    val invalid = results.collect { case Left(error) => error }
    val valid = results.collect { case Right(value)  => value }
    (invalid, valid) match {
      case (Nil, results)     => Right(results)
      case (invalidValues, _) => Left(invalidValues)
    }
  }
}

object QueryParamsUtils extends QueryParamsUtils
