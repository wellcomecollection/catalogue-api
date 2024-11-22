package weco.api.search.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.aggs.{
  AbstractAggregation,
  Aggregation,
  FilterAggregation,
  TermsAggregation,
  TermsOrder
}
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.term.TermsQuery
import weco.api.search.models._
import weco.api.search.models.request.{
  ImageAggregationRequest,
  WorkAggregationRequest
}

import scala.collection.immutable._

/** This class governs the way in which we wish to combine the filters and aggregations
  * that are specified for a search.
  *
  * We have a concept of "pairing" a filter and an aggregation:
  * for example, an aggregation on format is paired with a filter for a specific format.
  * If a search includes an aggregation and its paired filter, the filter is *not* applied to that
  * aggregation, but is still applied to results and to all other aggregations.
  *
  * Given a list of aggregation requests and filters, as well as functions to convert these to
  * constituents of the ES query.
  *
  * This class exposes:
  *
  * - `filteredAggregations`: a list of all the ES query aggregations.  Each aggregation is a Filter,
  *    applying all filters appropriate for the requested aggregation, containing the actual aggregation
  *    requested.  Additionally, if the aggregation is paired with a filter, it will also contain a subaggregation
  *    that ensures that the filtered-upon value is returned, regardless of the number of documents in which it is present
  */
trait FiltersAndAggregationsBuilder[Filter, AggregationRequest] {
  val aggregationRequests: List[AggregationRequest]
  val filters: List[Filter with Pairable]
  val requestToAggregation: AggregationRequest => Aggregation
  val filterToQuery: Filter => Query

  def pairedAggregationRequests(
    filter: Filter with Pairable
  ): List[AggregationRequest]

  /**
    * An aggregation that will contain the filtered-upon value even
    * if no documents in the aggregation context match it.
    */
  private def toSelfAggregation(
    agg: AbstractAggregation,
    filterQuery: Query
  ): AbstractAggregation =
    agg match {
      case terms: TermsAggregation =>
        filterQuery match {
          case TermsQuery(_, values, _, _, _, _, _) =>
            // The aggregation context may be excluding all documents
            // that match this filter term. Setting minDocCount to 0
            // allows the term in question to be returned.
            terms
              .minDocCount(0)
              .includeExactValues(values.map(value => value.toString).toSeq)
          case _ =>
            throw new NotImplementedError(
              "Only aggregations paired with terms filters have been implemented as paired aggregations"
            )
        }

      case _ =>
        throw new NotImplementedError(
          "Only terms aggregations have been implemented as paired aggregations"
        )
    }

  // Given an aggregation request, convert it to an actual aggregation
  // with a predictable sort order.
  // Higher count buckets come first, and within each group of higher count buckets,
  // the keys should be in alphabetical order.
  private def requestToOrderedAggregation(
    aggReq: AggregationRequest
  ): AbstractAggregation =
    requestToAggregation(aggReq) match {
      case terms: TermsAggregation =>
        terms.order(
          Seq(TermsOrder("_count", asc = false), TermsOrder("_key", asc = true))
        )
      case agg => agg
    }

  lazy val filteredAggregations: List[AbstractAggregation] =
    aggregationRequests.map { aggReq =>
      filteredAggregation(aggReq)
    }

  /**
    *  Turn an aggregation request into an actual aggregation.
    *  The aggregation will be filtered using all filters that do not operate on the same field as the aggregation (if any).
    *  It also contains a subaggregation that *does* additionally filter on that field.  This ensures that the filtered
    *  values are returned even if they fall outside the top n buckets as defined by the main aggregation.
    */
  private def filteredAggregation(
    aggReq: AggregationRequest
  ): FilterAggregation = {
    val agg = requestToOrderedAggregation(aggReq)
    pairedFilter(aggReq) match {
      case Some(paired) =>
        val otherFilters = filters.filterNot(_ == paired)
        val pairedQuery = filterToQuery(paired)
        // The top level aggregation filters the context by applying all the filters
        // operating on the other fields.
        filteredAggregation(
          // The requested aggregation operates within this filter.
          agg,
          otherFilters.map(filterToQuery),
          Seq(
            FilterAggregation(
              name = "self",
              // Then a further filter is applied, additionally applying the filter on this field.
              pairedQuery,
              // A modified version of the requested aggregation is applied again.
              // This ensures that the filtered-upon values are picked up, and will have the
              // correct document counts corresponding to all the filters.
              // Any of these that are present in the main aggregation will be exact duplicates.
              // Any that have been completely filtered out by other filters, will be present with a bucket count of 0
              // Any that have not been filtered out, but are still not in the top results (i.e. outside agg.size)
              //  will be returned and the counts will be influenced by all the filters.
              subaggs = Seq(toSelfAggregation(agg, pairedQuery))
            )
          )
        )
      case _ =>
        filteredAggregation(agg, filters.map(filterToQuery), Nil)
    }
  }

  private def filteredAggregation(
    agg: AbstractAggregation,
    filters: Seq[Query],
    extraSubaggs: Seq[AbstractAggregation]
  ): FilterAggregation =
    FilterAggregation(
      name = agg.name,
      boolQuery.filter {
        filters
      },
      subaggs = Seq(agg) ++ extraSubaggs
    )

  private def pairedFilter(
    aggregationRequest: AggregationRequest
  ): Option[Filter] =
    filters.find { filter =>
      pairedAggregationRequests(filter)
        .contains(aggregationRequest)
    }
}

class WorkFiltersAndAggregationsBuilder(
  val aggregationRequests: List[WorkAggregationRequest],
  val filters: List[WorkFilter with Pairable],
  val requestToAggregation: WorkAggregationRequest => Aggregation,
  val filterToQuery: WorkFilter => Query
) extends FiltersAndAggregationsBuilder[WorkFilter, WorkAggregationRequest] {

  override def pairedAggregationRequests(
    filter: WorkFilter with Pairable
  ): List[WorkAggregationRequest] =
    filter match {
      case _: FormatFilter       => List(WorkAggregationRequest.Format)
      case _: LanguagesFilter    => List(WorkAggregationRequest.Languages)
      case _: GenreFilter        => List(WorkAggregationRequest.Genre)
      case _: SubjectLabelFilter => List(WorkAggregationRequest.Subject)
      case _: ContributorsFilter => List(WorkAggregationRequest.Contributor)
      case _: LicenseFilter      => List(WorkAggregationRequest.License)
      case _: AvailabilitiesFilter =>
        List(WorkAggregationRequest.Availabilities)
    }

}

class ImageFiltersAndAggregationsBuilder(
  val aggregationRequests: List[ImageAggregationRequest],
  val filters: List[ImageFilter with Pairable],
  val requestToAggregation: ImageAggregationRequest => Aggregation,
  val filterToQuery: ImageFilter => Query
) extends FiltersAndAggregationsBuilder[ImageFilter, ImageAggregationRequest] {

  override def pairedAggregationRequests(
    filter: ImageFilter with Pairable
  ): List[ImageAggregationRequest] =
    filter match {
      case _: LicenseFilter => List(ImageAggregationRequest.License)
      case _: ContributorsFilter =>
        List(ImageAggregationRequest.SourceContributorAgents)
      case _: GenreFilter        => List(ImageAggregationRequest.SourceGenres)
      case _: SubjectLabelFilter => List(ImageAggregationRequest.SourceSubjects)
    }
}
