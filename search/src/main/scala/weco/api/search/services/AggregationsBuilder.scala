package weco.api.search.services

import com.sksamuel.elastic4s.ElasticApi.{boolQuery, matchAllQuery, termsAgg}
import com.sksamuel.elastic4s.requests.searches.aggs.{
  Aggregation,
  FilterAggregation,
  GlobalAggregation,
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

/**
  * Each aggregation follows the same nested aggregation structure to respect our aggregation and faceting principles, defined in RFC 37 (https://github.com/wellcomecollection/docs/tree/main/rfcs/037-api-faceting-principles):
  *  - [some.field.path]: A no-op filter aggregation which works as a container for sub-aggregations. Named in accordance with the requested field (see rule 3 in RFC 37).
  *    - 'filtered': A filter aggregation which applies all filters, *except for* filters paired to the aggregated field itself (see rule 5 in RFC 37).
  *        - 'nested': A nested aggregation which contains a 'labelled ID' sub-aggregation, or a 'label-only' sub-aggregation (see below for more info).
  *        - 'nestedSelf': Only included if a paired filter exists. Same as 'nested', but with a `includeExactValues` property set to the filtered values defined in the paired filter. This ensures that aggregation buckets corresponding to filtered values are always returned, even if they are not in the top N buckets returned by 'nested' (see rule 6 in RFC 37).
  *
  * To handle edge cases where a search with an applied filter returns 0 results, each aggregation also has an accompanying global aggregation:
  *  - '[some.field.path]Global': A global aggregation which ignores the query and all filters
  *    - 'nestedSelf': Same as the other (filtered) 'nestedSelf' but with no query/filters applied. Only included to cover the special case of a 'labelled ID' aggregation bucket returning an item count of 0, in which case the 'nestedSelf' aggregation is not able to determine the label matching the corresponding ID (see rule 6 in RFC 37).
  */
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
  ): Seq[Aggregation] =
    aggregationRequests.flatMap { aggregationRequest =>
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
  ): List[Aggregation] = {
    val toAggregation
      : (AggregationParams, String, List[String]) => Aggregation =
      params.aggregationType match {
        case AggregationType.LabeledIdAggregation => toLabelledIdAggregation
        case AggregationType.LabelOnlyAggregation => toLabelOnlyAggregation
      }

    val selfAggregation: Option[Aggregation] = pairedQuery match {
      case Some(TermsQuery(_, values, _, _, _, _, _)) =>
        val pairedValues = values.map(value => value.toString).toList
        Some(toAggregation(params, "nestedSelf", pairedValues))
      case _ => None
    }

    val filterAggregation = FilterAggregation(
      name = "filtered",
      boolQuery.filter(query),
      subaggs = Seq(
        Some(toAggregation(params, "nested", List())),
        selfAggregation).flatten
    )

    List(
      FilterAggregation(
        name = params.name,
        query = matchAllQuery(),
        subaggs = Seq(Some(filterAggregation)).flatten
      ),
      GlobalAggregation(
        name = params.name + "Global",
        subaggs = Seq(selfAggregation).flatten
      )
    )
  }

  private def toTermsAggregation(
    name: String,
    fieldPath: String,
    size: Int,
    include: List[String]
  ): TermsAggregation = {
    val aggregation = termsAgg(name, fieldPath)
      .size(size)
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

  /** Each aggregatable field is indexed as a nested field with an `id` value and a `label` value. Labeled ID aggregations
    * are `id`-based, with a `label`-based sub-aggregation to get Elasticsearch to return all `label` values associated
    * with each `id` bucket. (Usually each `id` value only has one one `label` value associated with it, but not always.
    * For example, different Works can use different labels for a given LoC Subject Heading.)
    */
  private def toLabelledIdAggregation(
    params: AggregationParams,
    nestedAggregationName: String,
    include: List[String]
  ): NestedAggregation = {
    val idAggregation =
      toTermsAggregation(
        "terms",
        s"${params.fieldPath}.id",
        params.size,
        include)
    val labelAggregation =
      termsAgg("labels", s"${params.fieldPath}.label").size(1)

    NestedAggregation(
      name = nestedAggregationName,
      path = params.fieldPath,
      subaggs = List(idAggregation.subAggregations(labelAggregation))
    )
  }

  /**
    * Label-only aggregations are based on the `label` subfield of each nested aggregatable field, the `id` subfield
    * is ignored.This aggregation type is included to keep supporting label-based concept aggregations, and can be
    * removed once we switch to ID-based aggregations.
    */
  private def toLabelOnlyAggregation(
    params: AggregationParams,
    nestedAggregationName: String,
    include: List[String]
  ): NestedAggregation = {
    val labelAggregation =
      toTermsAggregation(
        "terms",
        s"${params.fieldPath}.label",
        params.size,
        include)

    NestedAggregation(
      name = nestedAggregationName,
      path = params.fieldPath,
      subaggs = List(labelAggregation)
    )
  }
}
