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
  * that are specified for a search. We have a concept of "pairing" a filter and an aggregation:
  * for example, an aggregation on format is paired with a filter of a specific format.
  * If a search includes an aggregation and its paired filter, the filter is *not* applied to that
  * aggregation, but is still applied to results and to all other aggregations.
  *
  * Given a list of aggregations requests and filters, as well as functions to convert these to
  * constituents of the ES query, this class exposes:
  *
  * - `filteredAggregations`: a list of all the ES query aggregations, where those that need to be filtered
  *   now have a sub-aggregation of the filter aggregation type, named "filtered", and are in the global
  *   aggregation context so post-filtering of query results is not required.
  */
trait FiltersAndAggregationsBuilder[Filter, AggregationRequest] {
  val aggregationRequests: List[AggregationRequest]
  val filters: List[Filter]
  val requestToAggregation: AggregationRequest => Aggregation
  val filterToQuery: Filter => Query

  def pairedAggregationRequests(filter: Filter): List[AggregationRequest]

  // Ensure that characters like parentheses can still be returned by the self aggregation, escape any
  // regex tokens that might appear in filter terms.
  // (as present in some labels, e.g. /concepts/gafuyqgp: "Nicholson, Michael C. (Michael Christopher), 1962-")
  private def escapeRegexTokens(term: String): String =
    term.replaceAll("""([.?+*|{}\[\]()\\"])""", "\\\\$1")

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
        val filterTerm = filterQuery match {
          case TermsQuery(_, values, _, _, _, _, _) =>
            //Aggregable values are a JSON object encoded as a string
            // Filter terms correspond to a property of this JSON
            // object (normally id or label).
            // In order to ensure that this aggregation only matches
            // the whole value in question, it is enclosed in
            // escaped quotes.
            // This is not perfect, but should be sufficient.
            // If (e.g.) this filter operates
            // on id and there is a label for a different value
            // that exactly matches it, then both will be returned.
            // This is an unlikely scenario, and will still result
            // in the desired value being returned.
            s"""\\"(${values
              .map(value => escapeRegexTokens(value.toString))
              .mkString("|")})\\""""
          case _ => ""
        }
        // The aggregation context may be excluding all documents
        // that match this filter term. Setting minDocCount to 0
        // allows the term in question to be returned.
        terms.minDocCount(0).includeRegex(s".*($filterTerm).*")
      case agg => agg
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
    *  values are returned even if they fall outside the top n buckets as defined by the main aggregation
    */
  private def filteredAggregation(aggReq: AggregationRequest) = {
    val agg = requestToOrderedAggregation(aggReq)
    pairedFilter(aggReq) match {
      case Some(paired) =>
        val otherFilters = filters.filterNot(_ == paired)
        val pairedQuery = filterToQuery(paired)
        FilterAggregation(
          name = agg.name,
          boolQuery.filter {
            otherFilters.map(filterToQuery)
          },
          subaggs = Seq(
            agg,
            FilterAggregation(
              name = "self",
              pairedQuery,
              subaggs = Seq(toSelfAggregation(agg, pairedQuery))
            )
          )
        )
      case _ =>
        FilterAggregation(
          name = agg.name,
          boolQuery.filter {
            filters.map(filterToQuery)
          },
          subaggs = Seq(agg)
        )
    }
  }

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
  val filters: List[WorkFilter],
  val requestToAggregation: WorkAggregationRequest => Aggregation,
  val filterToQuery: WorkFilter => Query
) extends FiltersAndAggregationsBuilder[WorkFilter, WorkAggregationRequest] {

  override def pairedAggregationRequests(
    filter: WorkFilter
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
      case _ => Nil
    }
}

class ImageFiltersAndAggregationsBuilder(
  val aggregationRequests: List[ImageAggregationRequest],
  val filters: List[ImageFilter],
  val requestToAggregation: ImageAggregationRequest => Aggregation,
  val filterToQuery: ImageFilter => Query
) extends FiltersAndAggregationsBuilder[ImageFilter, ImageAggregationRequest] {

  override def pairedAggregationRequests(
    filter: ImageFilter
  ): List[ImageAggregationRequest] =
    filter match {
      case _: LicenseFilter => List(ImageAggregationRequest.License)
      case _                => Nil
    }
}
