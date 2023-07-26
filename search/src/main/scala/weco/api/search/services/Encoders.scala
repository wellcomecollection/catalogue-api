package weco.api.search.services

import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.requests.searches.aggs.{AbstractAggregation, AggregationBuilderFn}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import io.circe.{Encoder, Json, parser}
import io.circe.syntax.EncoderOps

import scala.annotation.nowarn

trait Encoders {
  // Encoders for turning E4S-style data into Circe JSON.
  // Currently, this simply uses E4S to generate the JSON as a string
  // and parse that back into JSON.
  // When we eventually eradicate E4S, these will have to create the JSON
  // directly

  // XContentBuilder.string is deprecated with an astoundingly unhelpful message
  // "will be replaced with a pluggable system", yet all the code in E4S
  // and its documentation still uses `.string()` everywhere.
  // This warning is being suppressed here as we do not intend to
  // carry on working this way for very long.
  @nowarn
  implicit val aggregationEncoder: Encoder[AbstractAggregation] = agg =>
    parser.parse(AggregationBuilderFn(agg).string()).getOrElse(Json.obj())

  implicit val aggregationsEncoder: Encoder[Seq[AbstractAggregation]] =
    aggs =>
      aggs.map(agg => agg.name -> agg.asJson).toMap.asJson

  @nowarn
  implicit val queryEncoder: Encoder[Query] = query =>
    parser.parse(QueryBuilderFn(query).string()).getOrElse(Json.obj())

}
