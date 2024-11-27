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
    filter: Filter with Pairable): List[AggregationRequest]
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
    aggregationRequests: List[AggregationRequest]): Seq[FilterAggregation] =
    aggregationRequests.map { aggregationRequest =>
      val aggregationParams = getAggregationParams(aggregationRequest)

      pairedFilter(filters, aggregationRequest) match {
        case Some(paired) =>
          val nonPairedQueries =
            filters.filterNot(_ == paired).map(buildFilterQuery)
          val pairedQuery = buildFilterQuery(paired)
          toFilterAggregation(
            aggregationParams,
            nonPairedQueries,
            Some(pairedQuery)
          )
        case _ =>
          toFilterAggregation(
            aggregationParams,
            filters.map(buildFilterQuery),
            None
          )
      }
    }
  private def toFilterAggregation(
    params: AggregationParams,
    query: List[Query],
    pairedQuery: Option[Query]
  ): FilterAggregation = {
    val toAggregation: (AggregationParams, List[String]) => Aggregation =
      params.aggregationType match {
        case AggregationType.LabeledIdAggregation => toLabeledIdAggregation
        case AggregationType.LabelOnlyAggregation => toTermsAggregation
      }

    val selfAggregation: Option[Aggregation] = pairedQuery match {
      case Some(TermsQuery(_, values, _, _, _, _, _)) =>
        val pairedValues = values.map(value => value.toString).toList
        Some(
          toAggregation(
            AggregationParams(
              "self",
              params.fieldPath,
              params.size,
              params.aggregationType),
            pairedValues))
      case _ => None
    }

    FilterAggregation(
      name = params.name,
      boolQuery.filter(query),
      subaggs = Seq(
        toAggregation(params, List()),
        selfAggregation.orNull
      ).filter(_ != null)
    )
  }

  private def toTermsAggregation(
    params: AggregationParams,
    include: List[String]
  ): TermsAggregation =
    termsAgg(params.name, params.fieldPath)
      .size(params.size)
      .order(
        Seq(
          TermsOrder("_count", asc = false),
          TermsOrder("_key", asc = true)
        )
      )
//      .minDocCount(1)
      .includeExactValues(include)

  /** Each aggregatable field is indexed as a nested field with an `id` value and a `label` value. All aggregations
    * are `id`-based, with a `label`-based sub-aggregation to get Elasticsearch to return all `label` values associated
    * with each `id` bucket. (Usually each `id` value only has one one `label` value associated with it, but not always.
    * For example, different Works can use different labels for a given LoC Subject Heading.)
    */
  private def toLabeledIdAggregation(
    params: AggregationParams,
    include: List[String]
  ): NestedAggregation = {
    val idAggregation =
      toTermsAggregation(
        AggregationParams(
          params.name,
          s"${params.fieldPath}.id",
          params.size,
          params.aggregationType),
        include)
    val labelAggregation =
      termsAgg("labels", s"${params.fieldPath}.label").size(1)
    val nestedAggregation = idAggregation.subAggregations(labelAggregation)

    val name = if (include.nonEmpty) {
      "nestedSelf"
    } else {
      "nested"
    }

    NestedAggregation(
      name = name,
      path = params.fieldPath,
      subaggs = List(nestedAggregation)
    )
  }
}
