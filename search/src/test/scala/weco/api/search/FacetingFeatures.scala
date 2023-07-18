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
  protected def Given[R](
    msg: String
  )(testWith: TestWith[JsonServer, R]): R
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
  protected val uncommonTerm: ScenarioData
  protected val multipleUncommonTerms: ScenarioData
  protected val queryingUncommonTerms: ScenarioData

  private def assertSameBuckets(
    expectedAggregations: Map[String, Seq[Json]],
    json: Json
  ): Assertion = {
    json.aggregationKeys should contain theSameElementsAs expectedAggregations.keys

    forEvery(expectedAggregations) {
      case (key, value) =>
        json.aggregationBuckets(key) shouldBe value
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

        val scenarioData = filterOneAggregateAnother
        Given("a dataset with aggregable values") { server =>
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
        val scenarioData = filterAndAggregateSame
        Given("a dataset with aggregable values") { server =>
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
        val scenarioData = filterMultiAndAggregateSame
        Given("a dataset with aggregable values") { server =>
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
        val scenarioData =
          filterAndAggregateMultiFields
        Given("a dataset with aggregable values") { server =>
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

      Rule(
        "the context for counting aggregation buckets is set by the query and filters"
      ) {
        // For Works in the catalogue api, this represents the Sex/London/Germany problem
        // Once filtered by subject, the subject aggregation was only returning the global top 20 terms
        // with the query applied as a filter afterwards.  This means that a less common (21+) value would not
        // be counted in the buckets (but would be returned with a zero count due to the associated filter rule)
        // This scenario shows that the less common value is to be counted properly and returned.
        Scenario("filtering on uncommon terms") {

          val scenarioData = uncommonTerm
          Given(
            "a dataset with some common aggregable values and a less common one"
          ) { server =>
            When("records are requested")
            And("the request is filtered on the least common aggregable value")
            And("asks for an aggregation on the same field")
            val responseJson = server.getJson(scenarioData.url)
            Then(
              "an aggregation with the correct count for that uncommon value is returned"
            )

            Then(
              "and the most common terms are also returned"
            )
            assertSameBuckets(
              scenarioData.expectedAggregationBuckets,
              responseJson
            )
          }
        }

        Scenario("filtering on multiple uncommon terms") {

          val scenarioData = multipleUncommonTerms
          Given(
            "a dataset with two uncommon terms in two different documents and some common terms that are not present in one of those documents"
          ) { server =>
            When("records are requested")
            And("the request is filtered on both uncommon terms")
            And("asks for an aggregation on the same field")
            val responseJson = server.getJson(scenarioData.url)
            Then(
              "aggregation buckets with the correct count for the uncommon values are returned"
            )

            Then(
              "and the most common terms are also returned"
            )
            assertSameBuckets(
              scenarioData.expectedAggregationBuckets,
              responseJson
            )
          }
        }

        Scenario("filtering on an uncommon term when querying") {
          val scenarioData = queryingUncommonTerms
          Given(
            "a dataset with two uncommon terms in two different documents and some common terms that are not present in one of those documents"
          ) { server =>
            When(
              "a query is made that excludes most of the top terms in the requested aggregation"
            )
            And(
              "a field is filtered on a value that is not in the set returned by the query"
            )
            And("an aggregation is requested on the same field")
            val responseJson = server.getJson(scenarioData.url)
            Then(
              "the buckets will contain the most common terms in the set returned by the query"
            )
            // This scenario could be improved by using a matcher that checks for a bucket with the
            // right key and a count of 0, but for now, trusting the inheriting suite to
            // specify the correct expected aggregation buckets will do.
            And(
              "the buckets will contain the filtered value with a count of zero"
            )
            assertSameBuckets(
              scenarioData.expectedAggregationBuckets,
              responseJson
            )

          }

        }
      }

      Rule(
        "buckets are ordered first by document count, then lexicographically"
      ) {
        // This rule can be covered by the scenarios within "context for counting aggregation buckets", above,
        // but could be expanded upon here if desired.
        // A Scenario that shows this requires a large set of equally common terms, along with some other
        // terms with a different frequency.  The simplest way to achieve the counting scenarios is to
        // have lots of terms in two documents, and one term that is only present in one of them.
      }

      Rule(
        "buckets with zero counts are only returned if they have an associated filter"
      ) {
        Scenario("empty buckets") {
          val scenarioData = emptyBucketFilter
          Given("a dataset with aggregable values") { server =>
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
          val scenarioData = mutexFilter
          Given("a dataset with aggregable values") { server =>
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
        val scenarioData = queryAndFilter
        Given("a dataset with aggregable values") { server =>
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
        val scenarioData = queryAndFilter
        Given("a dataset with aggregable values") { server =>
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
