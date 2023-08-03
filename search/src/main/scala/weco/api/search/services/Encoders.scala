package weco.api.search.services

import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.json.JacksonBuilder
import com.sksamuel.elastic4s.requests.searches.aggs.{
  AbstractAggregation,
  AggregationBuilderFn
}
import com.sksamuel.elastic4s.requests.searches.defaultCustomAggregationHandler
import com.sksamuel.elastic4s.requests.searches.queries.Query
import io.circe.{parser, Encoder, Json}
import io.circe.syntax.EncoderOps
import weco.api.search.models.request.SortingOrder

trait Encoders {
  // Encoders for turning E4S-style data into Circe JSON.
  // Currently, this simply uses E4S to generate the JSON as a string
  // and parse that back into JSON.
  // When we eventually eradicate E4S, these will have to create the JSON
  // directly

  implicit val aggregationEncoder: Encoder[AbstractAggregation] = agg =>
    parser
      .parse(
        JacksonBuilder.writeAsString(
          AggregationBuilderFn(agg, defaultCustomAggregationHandler).value
        )
      )
      .getOrElse(Json.obj())

  implicit val aggregationsEncoder: Encoder[Seq[AbstractAggregation]] =
    aggs => aggs.map(agg => agg.name -> agg.asJson).toMap.asJson

  implicit val queryEncoder: Encoder[Query] = query =>
    parser
      .parse(JacksonBuilder.writeAsString(QueryBuilderFn(query).value))
      .getOrElse(Json.obj())

  implicit val sortOrderEncoder: Encoder[SortingOrder] = {
    case SortingOrder.Ascending  => "asc".asJson
    case SortingOrder.Descending => "desc".asJson
  }
}
