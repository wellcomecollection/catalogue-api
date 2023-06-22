package weco.api.search
import io.circe.Json
import org.scalactic.source
import org.scalatest.{Assertion, GivenWhenThen, Informing, Inspectors}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.matchers.APIResponseMatchers
import weco.fixtures.TestWith

trait ExtraGherkinWords {
  this: Informing =>
  def Rule(
    description: String
  )(fun: => Unit)(implicit pos: source.Position): Unit = {
    info("")
    info(s"Rule: $description")(pos)
    //ideally, this would nest like Feature does, but scalatest has made
    // all that stuff private.
    fun
  }
}

trait FacetingFeatures
    extends AnyFeatureSpec
    with Inspectors
    with GivenWhenThen
    with ExtraGherkinWords
    with Matchers
    with APIResponseMatchers {
  protected trait JsonServer {
    def getJson(path: String): Json
  }
  protected val resourcePath: String
  protected def withFacetedAPI[R](testWith: TestWith[JsonServer, R]): R

  protected case class ScenarioData(
    queryTerm: Option[String] = None,
    filters: Seq[(String, String)] = Nil,
    aggregationFields: Seq[String] = Nil,
    expectedAggregationFields: Seq[String] = Nil,
    expectedAggregationBuckets: Map[String, Seq[Json]] = Map.empty
  ) {
    def formatFilters: String =
      filters.map(pair => s"${pair._1}=${pair._2}").mkString("&")
    def formatQuery: String =
      queryTerm match {
        case Some(term) => s"query=$term"
        case None       => ""
      }
    def formatAggregations: String =
      aggregationFields match {
        case Nil => ""
        case seq => s"aggregations=${seq.mkString(",")}"
      }
    def url: String =
      s"$resourcePath?${Seq(formatQuery, formatAggregations, formatFilters).mkString("&")}"
  }
  protected val oneAggregation: ScenarioData
  protected val twoAggregations: ScenarioData
  protected val queryAndAggregations: ScenarioData
  protected val filterOneAggregateAnother: ScenarioData
  protected val filterAndAggregateSame: ScenarioData
  protected val filterMultiAndAggregateSame: ScenarioData
  protected val filterAndAggregateMultiFields: ScenarioData

  private def assertSameBuckets(
    expectedAggregations: Map[String, Seq[Json]],
    json: Json
  ): Assertion = {
    json.aggregationKeys should contain theSameElementsAs expectedAggregations.keys

    forEvery(expectedAggregations) {
      case (key, value) =>
        json.aggregationBuckets(key) should contain theSameElementsAs value
    }
  }

  Feature("Faceting - aggregations and filters") {
    Rule("Aggregations are only returned when requested") {
      info(
        "  Aggregations are returned in the aggregations collection,"
      )
      info(
        "  they can be slow and expensive, and shouldn't happen if the client doesn't want them"
      )
      Scenario("a request with no aggregations") {

        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          When("records are requested")
          val responseJson = server.getJson(resourcePath)
          Then("no aggregations are returned")
          responseJson should not(containProperty("aggregations"))
        }
      }

      Scenario("a request with one aggregation") {
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = oneAggregation
          val field = scenarioData.aggregationFields.head
          When(
            s"records are requested with an aggregation on the '$field' field"
          )
          val responseJson =
            server.getJson(scenarioData.url)

          Then(
            s"only the '$field' aggregation will be returned"
          )
          responseJson.aggregations should have size 1
          And("all documents will have contributed to the bucket counts")
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }

      Scenario("a request with multiple aggregations") {
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = twoAggregations
          When("records are requested")
          val field1 = scenarioData.aggregationFields.head
          val field2 = scenarioData.aggregationFields(1)
          And(
            s"the request asks for aggregations on fields '$field1' and '$field2'"
          )
          val responseJson =
            server.getJson(s"$resourcePath?aggregations=$field1,$field2")
          Then(s"only aggregations for $field1 and $field2 will be returned")
          responseJson.aggregations should have size 2
          And("all documents will have contributed to the bucket counts")
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }
    }

    Rule(
      "Queries constrain the document collection over which aggregations operate"
    ) {
      Scenario("a request with a query and aggregations") {
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = queryAndAggregations
          val queryTerm = scenarioData.queryTerm.get
          When(s"a query is made for the term '$queryTerm'")
          val field1 = scenarioData.aggregationFields.head
          val field2 = scenarioData.aggregationFields(1)
          And(
            s"the request asks for aggregations on fields '$field1' and '$field2'"
          )
          val responseJson =
            server.getJson(
              s"$resourcePath?query=$queryTerm&aggregations=$field1,$field2"
            )
          Then(
            "only documents that match the query are counted in the aggregations"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }
    }

    Rule("Filters apply to all aggregated fields apart from their own") {

      Scenario("filtering one one field, and aggregating on another") {
        info("filters constrain the document collection for other aggregations")

        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = filterOneAggregateAnother
          When("records are requested")
          And(s"the request is filtered for ${scenarioData.formatFilters}")
          val aggregationField = scenarioData.aggregationFields.head
          And(s"asks for aggregation on $aggregationField")
          val responseJson = server.getJson(
            s"$resourcePath?${scenarioData.formatFilters}&aggregations=${scenarioData.aggregationFields.head}"
          )
          Then(
            "only documents that match the filter are counted in the aggregation"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }

      }

      Scenario("filtering and aggregating on the same field") {
        info(
          "filters do not constrain the document collection when aggregating on that same field"
        )
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = filterAndAggregateSame
          When("records are requested")
          And("the request has a filter")
          And("asks for aggregation on the same field")
          val aggregationField = scenarioData.aggregationFields.head
          aggregationField shouldBe scenarioData.filters.head._1
          val responseJson = server.getJson(
            s"$resourcePath?${scenarioData.formatFilters}&aggregations=${scenarioData.aggregationFields.head}"
          )
          Then(
            "documents that do not match that filter are also counted in the aggregation"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }

      Scenario("applying multiple filters to the same field") {
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = filterMultiAndAggregateSame
          When("records are requested")
          And("the request has two filters on the same field")
          val filterField = scenarioData.filters.head._1
          scenarioData.filters.map(_._1) shouldBe Seq(filterField, filterField)
          And("asks for an aggregation on the same field")
          val aggregationField = scenarioData.aggregationFields.head
          aggregationField shouldBe filterField
          val responseJson = server.getJson(
            s"$resourcePath?${scenarioData.formatFilters}&aggregations=${scenarioData.aggregationFields.head}"
          )
          Then(
            "documents that do not match either filter are also counted in the aggregation"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }

      Scenario("multiple filters and aggregations") {
        info(
          "filters apply to all aggregations but their own"
        )
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          val scenarioData = filterAndAggregateMultiFields
          When("records are requested")
          And("the request has filters on two different fields")
          And("asks for an aggregation on the same fields")
          scenarioData.filters.map(_._1) should contain theSameElementsAs scenarioData.aggregationFields
          val responseJson = server.getJson(scenarioData.url)
          And("asks for an aggregation on yet another field")
          // TODO: the filtered aggregations dataset doesn't cover this
          Then(
            "only documents that match the filters on other fields are counted in the aggregation on each field"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }

      Rule(
        "buckets with zero counts are only returned if they have an associated filter"
      ) {
        Scenario("requesting empty buckets") {
          Given("a dataset with aggregable values")
          When("records are requested")
          And(
            "the request has a filter on field A that excludes all resources with values in field B"
          )
          And("asks for an aggregation on field B")
          Then("no aggregations are returned for field B")
        }

        Scenario("requesting redundant filters") {
          info(
            "Combinations of filters and queries may result in some filters being redundant"
          )
          info("  i.e. that there are zero matches for it.")
          info(
            "   Any filter values with corresponding aggregations should be present in"
          )
          info("  the aggregation, regardless of bucket count being zero")
          Given("a dataset with aggregable values")
          When("records are requested")
          And(
            "the request has a filter on field A that excludes all resources with values in field B"
          )
          And("has a filter on field B")
          And("asks for an aggregation on field B")
          Then("the bucket corresponding to the field B filter is returned")
        }
      }
    }

    Rule("Queries and filters operate together") {

      Scenario("an aggregation with queries and filters") {
        Given("a dataset with aggregable values")
        When("a query is made")
        And("the request is filtered on one field")
        And("asks for aggregation on another field")
        Then(
          "only documents that match the filter and the query are counted in the aggregations"
        )
      }
    }

    Rule("Erroneous requests are rejected") {
      Scenario("an unknown aggregation") {
        Given("a dataset with aggregable values")
        When("records are requested")
        And("the request asks for an aggregation on a non-aggregable field")
        Then("a 400 error is returned")
      }

      Scenario("an unknown filter") {
        info(
          "currently, unexpected querystring parameters are ignored, should it be like this instead?"
        )
        Given("a dataset with aggregable values")
        When("records are requested")
        And("the request asks for a filter on a non-filterable field")
        Then("a 400 error is returned")

      }
    }
  }
}
