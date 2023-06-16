package weco.api.search
import io.circe.Json
import org.scalactic.source
import org.scalatest.{GivenWhenThen, Informing}
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
    with GivenWhenThen
    with ExtraGherkinWords
    with Matchers
    with APIResponseMatchers {
  protected trait JsonServer {
    def getJson(path: String): Json
  }
  protected val resourcePath: String
  protected def withFacetedAPI[R](testWith: TestWith[JsonServer, R]): R
  protected def aggregableFields: Seq[String]
  protected val queries: Seq[String]
  protected val buckets: Map[String, Seq[Json]]
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
          val singleAggregableField = aggregableFields.head
          When(
            s"records are requested with an aggregation on the '$singleAggregableField' field"
          )
          val responseJson =
            server.getJson(s"$resourcePath?aggregations=$singleAggregableField")

          Then(
            s"only the '$singleAggregableField' aggregation will be returned"
          )
          responseJson.aggregationKeys should contain theSameElementsAs Seq(
            singleAggregableField
          )
          And("all documents will have contributed to the bucket counts")
          val expectedBuckets = buckets(singleAggregableField)
          responseJson.aggregationBuckets(singleAggregableField) should contain theSameElementsAs expectedBuckets
        }
      }

      Scenario("a request with multiple aggregations") {
        Given("a dataset with aggregable values")
        withFacetedAPI { server =>
          When("records are requested")
          val field1 = aggregableFields.head
          val field2 = aggregableFields(1)
          And(
            s"the request asks for aggregations on fields '$field1' and '$field2'"
          )
          val responseJson =
            server.getJson(s"$resourcePath?aggregations=$field1,$field2")
          Then(s"only aggregations for $field1 and $field2 will be returned")
          responseJson.aggregationKeys should contain theSameElementsAs Seq(
            field1,
            field2
          )
          And("all documents will have contributed to the bucket counts")
          responseJson.aggregationBuckets(field1) should contain theSameElementsAs buckets(
            field1
          )
          responseJson.aggregationBuckets(field2) should contain theSameElementsAs buckets(
            field2
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
          When("a query is made with two aggregations")
          val queryText = queries.head
          val field1 = aggregableFields.head
          val field2 = aggregableFields(1)
          val responseJson =
            server.getJson(
              s"$resourcePath?query=$queryText&aggregations=$field1,$field2"
            )
          Then(
            "only documents that match the query are counted in the aggregations"
          )
          responseJson.aggregationBuckets(field1) should contain theSameElementsAs buckets(
            s"$queryText/$field1"
          )
          responseJson.aggregationBuckets(field2) should contain theSameElementsAs buckets(
            s"$queryText/$field2"
          )
        }
      }
    }

    Rule("Filters apply to all aggregated fields apart from their own") {

      Scenario("filtering one one field, and aggregating on another") {
        info("filters constrain the document collection for other aggregations")
        Given("a dataset with aggregable values")
        When("records are requested")
        And("the request is filtered on one field")
        And("asks for aggregation on another field")
        Then(
          "only documents that match the filter are counted in the aggregation"
        )

      }

      Scenario("filtering and aggregating on the same field") {
        info(
          "filters do not constrain the document collection when aggregating on that same field"
        )
        Given("a dataset with aggregable values")
        When("records are requested")
        And("the request has a filter")
        And("asks for aggregation on the same field")
        Then(
          "documents that do not match that filter are also counted in the aggregation"
        )
      }

      Scenario("applying multiple filters to the same field") {
        Given("a dataset with aggregable values")
        When("records are requested")
        And("the request has two filters on the same field")
        And("asks for an aggregation on the same field")
        Then(
          "documents that do not match either filter are also counted in the aggregation"
        )
      }

      Scenario("multiple filters and aggregations") {
        info(
          "filters apply to all aggregations but their own"
        )
        Given("a dataset with aggregable values")
        When("records are requested")
        And("the request has filters on two different fields")
        And("asks for an aggregation on the same fields")
        And("asks for an aggregation on yet another field")
        Then(
          "only documents that match the filters on other fields are counted in the aggregation on each field"
        )
      }

      Rule(
        "buckets with zero counts are only returned if they have an associated filter"
      ) {
        Scenario("requesting empty buckets") {
          Given("a dataset with aggregable values")
          When("records are requested")
          And(
            "the request has a filter on field A that excludes all resources with values in field B "
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
            "the request has a filter on field A that excludes all resources with values in field B "
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
