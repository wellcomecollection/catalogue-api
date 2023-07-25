package weco.api.search.services

import com.sksamuel.elastic4s.requests.searches.aggs.{
  AbstractAggregation,
  AggregationBuilderFn
}
import io.circe.{parser, Encoder, Json}
import io.circe.syntax.EncoderOps

import scala.annotation.nowarn

trait Encoders {
  @nowarn
  implicit val aggregationEncoder: Encoder[AbstractAggregation] = agg =>
    parser.parse(AggregationBuilderFn(agg).string()).getOrElse(Json.obj())

  implicit val aggregationsEncoder: Encoder[Seq[AbstractAggregation]] = {
    aggs =>
      aggs.map(agg => agg.name -> agg.asJson).toMap.asJson
  }

}
