package uk.ac.wellcome.platform.api

import java.time.LocalDate

import org.scalatest.{FunSpec, Matchers, OptionValues}
import uk.ac.wellcome.display.models.{
  AggregationRequest,
  SortRequest,
  SortingOrder,
  V2WorksIncludes
}
import uk.ac.wellcome.platform.api.models.{
  GenreFilter,
  ItemLocationTypeFilter,
  LanguageFilter,
  LicenseFilter,
  SubjectFilter,
  WorkQuery,
  WorkQueryType,
  WorkTypeFilter
}
import uk.ac.wellcome.platform.api.models.WorkQueryType._

class QueryParamsTest extends FunSpec with Matchers with OptionValues {
  describe("MultipleWorksParams") {
    it("has default workQuery type of MSMBoostQuery") {
      val params = createMultipleWorksParams(query = Some("Royal Redcurrant"))

      params.workQuery.value shouldEqual WorkQuery(
        queryString = "Royal Redcurrant",
        queryType = MSMBoostQuery
      )
    }

    it(
      "has workQuery type of MSMBoostQueryUsingAndOperator if `_queryType` is 'usingAnd'") {
      val params = createMultipleWorksParams(
        query = Some("Royal Redcurrant"),
        _queryType = Some(MSMBoostQueryUsingAndOperator)
      )

      params.workQuery.value shouldEqual WorkQuery(
        queryString = "Royal Redcurrant",
        queryType = MSMBoostQueryUsingAndOperator
      )
    }
  }

  def createMultipleWorksParams(
    page: Option[Int] = None,
    pageSize: Option[Int] = None,
    workType: Option[WorkTypeFilter] = None,
    `items.locations.locationType`: Option[ItemLocationTypeFilter] = None,
    `production.dates.from`: Option[LocalDate] = None,
    `production.dates.to`: Option[LocalDate] = None,
    language: Option[LanguageFilter] = None,
    `genres.label`: Option[GenreFilter] = None,
    `subjects.label`: Option[SubjectFilter] = None,
    license: Option[LicenseFilter] = None,
    include: Option[V2WorksIncludes] = None,
    aggregations: Option[List[AggregationRequest]] = None,
    sort: Option[List[SortRequest]] = None,
    sortOrder: Option[SortingOrder] = None,
    query: Option[String] = None,
    _queryType: Option[WorkQueryType] = None,
    _index: Option[String] = None,
  ): MultipleWorksParams = MultipleWorksParams(
    page = page,
    pageSize = pageSize,
    workType = workType,
    `items.locations.locationType` = `items.locations.locationType`,
    `production.dates.from` = `production.dates.from`,
    `production.dates.to` = `production.dates.to`,
    language = language,
    `genres.label` = `genres.label`,
    `subjects.label` = `subjects.label`,
    license = license,
    include = include,
    aggregations = aggregations,
    sort = sort,
    sortOrder = sortOrder,
    query = query,
    _queryType = _queryType,
    _index = _index,
  )
}
