package weco.api.search
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
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
    def failToGet(path: String): StatusCode
  }

  protected val resourcePath: String
  protected def withFacetedAPI[R](testWith: TestWith[JsonServer, R]): R

  protected def Given[R](msg: String)(
    testWith: TestWith[JsonServer, R]
  ): R

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
  protected val mutexFilter: ScenarioData
  protected val emptyBucketFilter: ScenarioData
  protected val queryAndFilter: ScenarioData

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

        Given("a dataset with aggregable values") { server =>
          When("records are requested")
          val responseJson = server.getJson(resourcePath)
          Then("no aggregations are returned")
          responseJson should not(containProperty("aggregations"))
        }
      }

      Scenario("a request with one aggregation") {
        val scenarioData = oneAggregation
        Given("a dataset with multiple aggregable fields") { server =>
          val field = scenarioData.aggregationFields.head
          When(
            s"records are requested with an aggregation on the '$field' field"
          )
          val responseJson = server.getJson(scenarioData.url)

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
        val scenarioData = twoAggregations
        Given("a dataset with multiple aggregable fields") { server =>
          When("records are requested")
          val field1 = scenarioData.aggregationFields.head
          val field2 = scenarioData.aggregationFields(1)
          And(
            s"the request asks for aggregations on fields '$field1' and '$field2'"
          )
          val responseJson = server.getJson(scenarioData.url)
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
        val scenarioData = queryAndAggregations
        Given("a dataset with queryable content and multiple aggregable fields") {
          server =>
            val queryTerm = scenarioData.queryTerm.get
            When(s"a query is made for the term '$queryTerm'")
            val field1 = scenarioData.aggregationFields.head
            val field2 = scenarioData.aggregationFields(1)
            And(
              s"the request asks for aggregations on fields '$field1' and '$field2'"
            )
            val responseJson = server.getJson(scenarioData.url)
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

        Given("a dataset with aggregable values") { server =>
          val scenarioData = filterOneAggregateAnother
          When("records are requested")
          And(s"the request is filtered for ${scenarioData.formatFilters}")
          val aggregationField = scenarioData.aggregationFields.head
          And(s"asks for aggregation on $aggregationField")
          val responseJson = server.getJson(scenarioData.url)
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
        Given("a dataset with aggregable values") { server =>
          val scenarioData = filterAndAggregateSame
          When("records are requested")
          And("the request has a filter")
          And("asks for aggregation on the same field")
          val aggregationField = scenarioData.aggregationFields.head
          aggregationField shouldBe scenarioData.filters.head._1
          val responseJson = server.getJson(scenarioData.url)
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
        Given("a dataset with aggregable values") { server =>
          val scenarioData = filterMultiAndAggregateSame
          When("records are requested")
          And("the request has two filters on the same field")
          val filterField = scenarioData.filters.head._1
          scenarioData.filters.map(_._1) shouldBe Seq(filterField, filterField)
          And("asks for an aggregation on the same field")
          val aggregationField = scenarioData.aggregationFields.head
          aggregationField shouldBe filterField
          val responseJson = server.getJson(scenarioData.url)
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
        Given("a dataset with aggregable values") { server =>
          val scenarioData = filterAndAggregateMultiFields
          When("records are requested")
          And(
            s"the request has filters on two different fields, ${scenarioData.filters.map(_._1).mkString(" and ")}"
          )
          And("asks for an aggregation on the same fields")
          scenarioData.filters.map(_._1) should contain theSameElementsAs scenarioData.aggregationFields
          val responseJson = server.getJson(scenarioData.url)
          Then(
            "only documents that match the filters on other fields are counted in the aggregation on each field"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }

      Scenario("filtering on an unpopular term") {
        // For Works in the catalogue api, this represents the Sex/London/Germany problem
        // Once filtered by subject, the subject aggregation was only returning the global top 20 terms
        // with the query applied as a filter afterwards.  This means that a less common (21+) value would not
        // be counted in the buckets (but would be returned with a zero count due to the associated filter rule)
        // This scenario shows that the less common value is to be counted properly and returned.
        Given(
          "a dataset with some common aggregable values and a less common one"
        ) { _ =>
          When("records are requested")
          And("the request is filtered on the least common aggregable value")
          And("asks for an aggregation on the same field")
          Then(
            "an aggregation with the correct count for that uncommon value is returned"
          )
        }

      }

      Rule(
        "buckets are ordered first by document count, then lexicographically"
      ) {
        Scenario("ordering buckets in an aggregation without a filter") {}
        Scenario("ordering buckets in an aggregation with a filter") {}

      }

      Rule(
        "buckets with zero counts are only returned if they have an associated filter"
      ) {
        Scenario("empty buckets") {
          Given("a dataset with aggregable values") { server =>
            val scenarioData = emptyBucketFilter
            When("records are requested")
            And(
              "the request has a filter on field A that excludes all resources with values in field B"
            )
            And("asks for an aggregation on field B")
            val responseJson = server.getJson(scenarioData.url)
            Then("no aggregation buckets are returned for field B")
            responseJson.aggregationBuckets(
              scenarioData.expectedAggregationBuckets.head._1
            ) shouldBe Nil
          }
        }

        Scenario("the requested filters are mutually exclusive") {
          Given("a dataset with aggregable values") { server =>
            val scenarioData = mutexFilter
            When("records are requested")
            val emptyFilter = scenarioData.filters.head
            And(
              s"the request has a filter for ${emptyFilter._1} being ${emptyFilter._2}"
            )
            val exclusionFilter = scenarioData.filters(1)
            And(
              s"the request has a filter for ${exclusionFilter._1} being ${exclusionFilter._2}"
            )
            And("those filters are mutually exclusive")
            And("asks for an aggregation on both fields")
            val responseJson = server.getJson(scenarioData.url)
            Then(
              "only documents that match the filters on other fields are counted in the aggregation on each field"
            )
            And(
              "the aggregations will contain zero-count buckets for the two requested filters"
            )
            assertSameBuckets(
              scenarioData.expectedAggregationBuckets,
              responseJson
            )
          }
        }
      }
    }

    Rule("Queries and filters operate together") {

      Scenario("an aggregation with queries and filters") {
        Given("a dataset with aggregable values") { server =>
          val scenarioData = queryAndFilter
          When("a query is made")
          And("the request is filtered on one field")
          And("asks for aggregation on another field")
          val responseJson = server.getJson(scenarioData.url)
          Then(
            "only documents that match the filter and the query are counted in the aggregations"
          )
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }

      Scenario("unexpected parameters") {
        Given("a dataset with aggregable values") { server =>
          val scenarioData = queryAndFilter
          When("a query is made")
          And("the request contains unknown parameters")
          val responseJson =
            server.getJson(s"${scenarioData.url}&thisIsNotAFilter=SomeValue")
          Then("the unknown parameters are ignored")
          assertSameBuckets(
            scenarioData.expectedAggregationBuckets,
            responseJson
          )
        }
      }
    }

    Rule("Erroneous requests are rejected") {
      Scenario("an unknown aggregation") {
        Given("a dataset with aggregable values") { server: JsonServer =>
          When("records are requested")
          And("the request asks for an aggregation on a non-aggregable field")
          val responseCode = server.failToGet(
            s"$resourcePath?aggregations=anAggregationThatDoesNotExist"
          )
          Then("a 400 error is returned")
          responseCode shouldBe StatusCodes.BadRequest
        }
      }
    }
  }
}
