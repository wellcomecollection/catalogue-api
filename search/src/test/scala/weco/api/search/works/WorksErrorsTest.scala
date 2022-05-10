package weco.api.search.works

import com.sksamuel.elastic4s.Index
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.models.ElasticConfig
import weco.elasticsearch.IndexConfig

class WorksErrorsTest extends ApiWorksTestBase with TableDrivenPropertyChecks {

  val includesString =
    "['identifiers', 'items', 'holdings', 'subjects', 'genres', 'contributors', 'production', 'languages', 'notes', 'images', 'parts', 'partOf', 'precededBy', 'succeededBy']"

  describe("returns a 400 Bad Request for errors in the ?include parameter") {
    it("a single invalid include") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?include=foo",
          description =
            s"include: 'foo' is not a valid value. Please choose one of: $includesString"
        )
      }
    }

    it("multiple invalid includes") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?include=foo,bar",
          description =
            s"include: 'foo', 'bar' are not valid values. Please choose one of: $includesString"
        )
      }
    }

    it("a mixture of valid and invalid includes") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?include=foo,identifiers,bar",
          description =
            s"include: 'foo', 'bar' are not valid values. Please choose one of: $includesString"
        )
      }
    }

    it("an invalid include on an individual work") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works/nfdn7wac?include=foo",
          description =
            s"include: 'foo' is not a valid value. Please choose one of: $includesString"
        )
      }
    }
  }

  val aggregationsString =
    "['workType', 'genres.label', 'production.dates', 'subjects.label', 'languages', 'contributors.agent.label', 'items.locations.license', 'availabilities']"

  describe(
    "returns a 400 Bad Request for errors in the ?aggregations parameter"
  ) {
    it("a single invalid aggregation") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?aggregations=foo",
          description =
            s"aggregations: 'foo' is not a valid value. Please choose one of: $aggregationsString"
        )
      }
    }

    it("multiple invalid aggregations") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?aggregations=foo,bar",
          description =
            s"aggregations: 'foo', 'bar' are not valid values. Please choose one of: $aggregationsString"
        )
      }
    }

    it("a mixture of valid and invalid aggregations") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?aggregations=foo,workType,bar",
          description =
            s"aggregations: 'foo', 'bar' are not valid values. Please choose one of: $aggregationsString"
        )
      }
    }
  }

  it("multiple invalid sorts") {
    withApi { route =>
      assertBadRequest(route)(
        path = s"$rootPath/works?sort=foo,bar",
        description =
          "sort: 'foo', 'bar' are not valid values. Please choose one of: ['production.dates']"
      )
    }
  }

  describe("returns a 400 Bad Request for errors in the ?sort parameter") {
    it("a single invalid sort") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?sort=foo",
          description =
            "sort: 'foo' is not a valid value. Please choose one of: ['production.dates']"
        )
      }
    }

    it("multiple invalid sorts") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?sort=foo,bar",
          description =
            "sort: 'foo', 'bar' are not valid values. Please choose one of: ['production.dates']"
        )
      }
    }

    it("a mixture of valid and invalid sort") {
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?sort=foo,production.dates,bar",
          description =
            "sort: 'foo', 'bar' are not valid values. Please choose one of: ['production.dates']"
        )
      }
    }
  }

  // This is expected as it's transient parameter that will have valid values changing over time
  // And if there is a client with a deprecated value, we wouldn't want it to fail
  describe("returns a 200 for invalid values in the ?_queryType parameter") {
    it("200s despite being a unknown value") {
      withWorksApi {
        case (_, route) =>
          assertJsonResponse(
            route,
            path =
              s"$rootPath/works?_queryType=athingwewouldneverusebutmightbecausewesaidwewouldnot"
          ) {
            Status.OK -> emptyJsonResult
          }
      }
    }
  }

  describe("returns a 400 Bad Request for user errors") {
    describe("errors in the ?pageSize query") {
      it("not an integer") {
        val pageSize = "penguin"
        withApi { route =>
          assertBadRequest(route)(
            path = s"$rootPath/works?pageSize=$pageSize",
            description = s"pageSize: must be a valid Integer"
          )
        }
      }

      it("just over the maximum") {
        val pageSize = 101
        withApi { route =>
          assertBadRequest(route)(
            path = s"$rootPath/works?pageSize=$pageSize",
            description = "pageSize: must be between 1 and 100"
          )
        }
      }

      it("just below the minimum (zero)") {
        val pageSize = 0
        withApi { route =>
          assertBadRequest(route)(
            path = s"$rootPath/works?pageSize=$pageSize",
            description = "pageSize: must be between 1 and 100"
          )
        }
      }

      it("a large page size") {
        val pageSize = 1000
        withApi { route =>
          assertBadRequest(route)(
            path = s"$rootPath/works?pageSize=$pageSize",
            description = "pageSize: must be between 1 and 100"
          )
        }
      }

      it("a negative page size") {
        val pageSize = -50
        withApi { route =>
          assertBadRequest(route)(
            path = s"$rootPath/works?pageSize=$pageSize",
            description = "pageSize: must be between 1 and 100"
          )
        }
      }
    }

    describe("errors in the ?page query") {
      it("page 0") {
        val page = 0
        withApi { route =>
          assertBadRequest(route)(
            path = s"$rootPath/works?page=$page",
            description = "page: must be greater than 1"
          )
        }
      }

      it("a negative page") {
        val page = -50
        assertIsBadRequest(
          s"$rootPath/works?page=$page",
          description = "page: must be greater than 1"
        )
      }
    }

    describe("trying to get more works than ES allows") {
      val description = "Only the first 10000 works are available in the API. " +
        "If you want more works, you can download a snapshot of the complete catalogue: " +
        "https://developers.wellcomecollection.org/datasets"

      it("a very large page") {
        withWorksApi {
          case (_, route) =>
            assertBadRequest(route)(
              path = s"$rootPath/works?page=10000",
              description = description
            )
        }
      }

      // This is a regression test for https://github.com/wellcometrust/platform/issues/3233
      //
      // We saw real requests like this, which we traced to an overflow in the
      // page offset we were requesting in Elasticsearch.
      it("so many pages that a naive (page * pageSize) would overflow") {
        withWorksApi {
          case (_, route) =>
            assertBadRequest(route)(
              path = s"$rootPath/works?page=2000000000&pageSize=100",
              description = description
            )
        }
      }

      it("the 101th page with 100 results per page") {
        withWorksApi {
          case (_, route) =>
            assertBadRequest(route)(
              path = s"$rootPath/works?page=101&pageSize=100",
              description = description
            )
        }
      }
    }

    it("returns multiple errors if there's more than one invalid parameter") {
      val pageSize = -60
      val page = -50
      withApi { route =>
        assertBadRequest(route)(
          path = s"$rootPath/works?pageSize=$pageSize&page=$page",
          description =
            "page: must be greater than 1, pageSize: must be between 1 and 100"
        )
      }
    }

    // This test is a best-effort regression test for a real query we saw, which caused
    // the following error in Elasticsearch:
    //
    //       "caused_by": {
    //         "caused_by": {
    //           "reason": "Failed to parse with all enclosed parsers",
    //           "type": "date_time_parse_exception"
    //         },
    //       "reason": "failed to parse date field [+11860-01-01] with format [strict_date_optional_time||epoch_millis]",
    //       "type": "illegal_argument_exception"
    //
    // Trying to reproduce that exact error with a local Elasticsearch is hard: it only
    // seems to occur with at least a few thousand works in the index, and not consistently.
    //
    // This test confirms that we do something sensible with the query parameters, but
    // it's possible that there are other scenarios in which this Elasticsearch exception
    // could arise which we aren't covering.
    //
    it("if the date is too large") {
      withWorksApi {
        case (_, route) =>
          assertBadRequest(route)(
            path =
              s"$rootPath/works?_queryType=undefined&production.dates.from=%2B011860-01-01",
            description = "production.dates.from: year must be less than 9999"
          )
      }
    }
  }

  describe("returns a 404 Not Found for missing resources") {
    it("looking up a work that doesn't exist") {
      val badId = "doesnotexist"
      withWorksApi {
        case (_, route) =>
          assertNotFound(route)(
            path = s"$rootPath/works/$badId",
            description = s"Work not found for identifier $badId"
          )
      }
    }

    it("looking up a work with a malformed identifier") {
      val badId = "zd224ncv]"
      withWorksApi {
        case (_, route) =>
          assertNotFound(route)(
            path = s"$rootPath/works/$badId",
            description = s"Work not found for identifier $badId"
          )
      }
    }
  }

  it("returns a 500 error if the default index doesn't exist") {
    val testPaths = Table(
      "path",
      s"$rootPath/works",
      s"$rootPath/works?query=fish",
      s"$rootPath/works/xyakm37j"
    )

    withApi { route =>
      forAll(testPaths) { path =>
        assertJsonResponse(route, path)(
          Status.InternalServerError ->
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 500,
               |  "label": "Internal Server Error"
               |}
            """.stripMargin
        )
      }
    }
  }

  it("returns an Internal Server error if you try to search a malformed index") {
    // We need to do something that reliably triggers an internal exception
    // in the Elasticsearch handler.
    //
    // By creating an index without a mapping, we don't have a canonicalId field
    // to sort on.  Trying to query this index will trigger one such exception!
    withLocalElasticsearchIndex(config = IndexConfig.empty) { worksIndex =>
      val elasticConfig = ElasticConfig(
        worksIndex = worksIndex,
        imagesIndex = Index("imagesIndex-notused")
      )

      withRouter(elasticConfig) { route =>
        val path = s"$rootPath/works"
        assertJsonResponse(route, path)(
          Status.InternalServerError ->
            s"""
               |{
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 500,
               |  "label": "Internal Server Error"
               |}
            """.stripMargin
        )
      }
    }
  }
}
