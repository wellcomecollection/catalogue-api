package weco.api.search.models

import com.sksamuel.elastic4s.requests.searches.SearchResponse

case class WorkAggregations(
  format: Option[Aggregation] = None,
  genresLabel: Option[Aggregation] = None,
  productionDates: Option[Aggregation] = None,
  languages: Option[Aggregation] = None,
  archiveCategory: Option[Aggregation] = None,
  collectionRoot: Option[Aggregation] = None,
  subjectsLabel: Option[Aggregation] = None,
  contributorsAgentsLabel: Option[Aggregation] = None,
  itemsLocationsLicense: Option[Aggregation] = None,
  itemsLocationsAccessConditionsMethod: Option[Aggregation] = None,
  availabilities: Option[Aggregation] = None
)

object WorkAggregations extends ElasticAggregations {
  def apply(searchResponse: SearchResponse): Option[WorkAggregations] = {
    val e4sAggregations = searchResponse.aggregations
    if (e4sAggregations.data.nonEmpty) {
      Some(
        WorkAggregations(
          format = e4sAggregations.decodeAgg("format"),
          genresLabel = e4sAggregations.decodeAgg("genres"),
          productionDates = e4sAggregations.decodeAgg("productionDates"),
          languages = e4sAggregations.decodeAgg("languages"),
          archiveCategory = e4sAggregations.decodeAgg("archiveCategory"),
          collectionRoot = e4sAggregations.decodeAgg("collectionRoot"),
          subjectsLabel = e4sAggregations.decodeAgg("subjects"),
          // TODO decode only agents here once `contributors` is removed
          contributorsAgentsLabel = e4sAggregations.decodeAgg("contributors"),
          itemsLocationsLicense = e4sAggregations.decodeAgg("license"),
          itemsLocationsAccessConditionsMethod =
            e4sAggregations.decodeAgg("accessMethod"),
          availabilities = e4sAggregations.decodeAgg("availabilities")
        )
      )
    } else {
      None
    }
  }
}
