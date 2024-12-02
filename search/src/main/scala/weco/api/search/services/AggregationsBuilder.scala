package weco.api.search.services

import com.sksamuel.elastic4s.ElasticApi.{boolQuery, termsAgg}
import com.sksamuel.elastic4s.requests.searches.aggs.{
  Aggregation,
  FilterAggregation,
  NestedAggregation,
  TermsAggregation,
  TermsOrder
}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.term.TermsQuery
import weco.api.search.models.Pairable

sealed trait AggregationType

object AggregationType {
  case object LabeledIdAggregation extends AggregationType
  case object LabelOnlyAggregation extends AggregationType
}

case class AggregationParams(
  name: String,
  fieldPath: String,
  size: Int,
  aggregationType: AggregationType
)

trait AggregationsBuilder[AggregationRequest, Filter] {
  def pairedAggregationRequests(
    filter: Filter with Pairable
  ): List[AggregationRequest]
  def getAggregationParams(request: AggregationRequest): AggregationParams
  def buildFilterQuery: PartialFunction[Filter, Query]

  private def pairedFilter(
    pairables: List[Filter with Pairable],
    aggregationRequest: AggregationRequest
  ): Option[Filter] =
    pairables.find { filter =>
      pairedAggregationRequests(filter)
        .contains(aggregationRequest)
    }

  def getAggregations(
    filters: List[Filter with Pairable],
    aggregationRequests: List[AggregationRequest]
  ): Seq[FilterAggregation] =
    aggregationRequests.map { aggregationRequest =>
      val aggregationParams = getAggregationParams(aggregationRequest)

      val (queries, pairedQuery) =
        pairedFilter(filters, aggregationRequest) match {
          case Some(paired) =>
            val nonPairedQueries =
              filters.filterNot(_ == paired).map(buildFilterQuery)
            (nonPairedQueries, Some(buildFilterQuery(paired)))
          case _ =>
            (filters.map(buildFilterQuery), None)
        }

      toFilterAggregation(aggregationParams, queries, pairedQuery)
    }

  private def toFilterAggregation(
    params: AggregationParams,
    query: List[Query],
    pairedQuery: Option[Query]
  ): FilterAggregation = {
    val toAggregation: (AggregationParams, String, List[String]) => Aggregation =
      params.aggregationType match {
        case AggregationType.LabeledIdAggregation => toLabeledIdAggregation
        case AggregationType.LabelOnlyAggregation => toLabelOnlyAggregation
      }

    val selfAggregation: Option[Aggregation] = pairedQuery match {
      case Some(TermsQuery(_, values, _, _, _, _, _)) =>
        val pairedValues = values.map(value => value.toString).toList
        Some(toAggregation(params, "nestedSelf", pairedValues))
      case _ => None
    }

    FilterAggregation(
      name = params.name,
      boolQuery.filter(query),
      subaggs = Seq(Some(toAggregation(params, "nested", List())), selfAggregation).flatten
    )
  }

  private def toTermsAggregation(
    params: AggregationParams,
    include: List[String]
  ): TermsAggregation = {
    val aggregation = termsAgg(params.name, params.fieldPath)
      .size(params.size)
      .order(
        Seq(
          TermsOrder("_count", asc = false),
          TermsOrder("_key", asc = true)
        )
      )

    // This handles self-aggregations
    if (include.nonEmpty) aggregation.includeExactValues(include).minDocCount(0)
    else aggregation
  }

  /** Each aggregatable field is indexed as a nested field with an `id` value and a `label` value. All aggregations
    * are `id`-based, with a `label`-based sub-aggregation to get Elasticsearch to return all `label` values associated
    * with each `id` bucket. (Usually each `id` value only has one one `label` value associated with it, but not always.
    * For example, different Works can use different labels for a given LoC Subject Heading.)
    */
  private def toLabeledIdAggregation(
    params: AggregationParams,
    nestedAggregationName: String,
    include: List[String]
  ): NestedAggregation = {
    val idAggregation =
      toTermsAggregation(params.copy(fieldPath = s"${params.fieldPath}.id"), include)
    val labelAggregation =
      termsAgg("labels", s"${params.fieldPath}.label").size(1)

    NestedAggregation(
      name = nestedAggregationName,
      path = params.fieldPath,
      subaggs = List(idAggregation.subAggregations(labelAggregation))
    )
  }

  private def toLabelOnlyAggregation(
    params: AggregationParams,
    nestedAggregationName: String,
    include: List[String]
  ): NestedAggregation = {
    val labelAggregation =
      toTermsAggregation(params.copy(fieldPath = s"${params.fieldPath}.label"), include)

    NestedAggregation(
      name = nestedAggregationName,
      path = params.fieldPath,
      subaggs = List(labelAggregation)
    )
  }
}
