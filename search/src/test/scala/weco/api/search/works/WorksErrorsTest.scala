package weco.api.search.works

import org.scalatest.Assertion

class WorksErrorsTest extends ApiWorksTestBase {

  val includesString =
    "['identifiers', 'items', 'holdings', 'subjects', 'genres', 'contributors', 'production', 'languages', 'notes', 'images', 'parts', 'partOf', 'precededBy', 'succeededBy']"

  describe("returns a 400 Bad Request for errors in the ?include parameter") {
    it("a single invalid include") {
      assertIsBadRequest(
        s"$rootPath/works?include=foo",
        description =
          s"include: 'foo' is not a valid value. Please choose one of: $includesString"
      )
    }

    it("multiple invalid includes") {
      assertIsBadRequest(
        s"$rootPath/works?include=foo,bar",
        description =
          s"include: 'foo', 'bar' are not valid values. Please choose one of: $includesString"
      )
    }

    it("a mixture of valid and invalid includes") {
      assertIsBadRequest(
        s"$rootPath/works?include=foo,identifiers,bar",
        description =
          s"include: 'foo', 'bar' are not valid values. Please choose one of: $includesString"
      )
    }

    it("an invalid include on an individual work") {
      assertIsBadRequest(
        s"$rootPath/works/nfdn7wac?include=foo",
        description =
          s"include: 'foo' is not a valid value. Please choose one of: $includesString"
      )
    }
  }

  val aggregationsString =
    "['workType', 'genres.label', 'production.dates', 'subjects.label', 'languages', 'contributors.agent.label', 'items.locations.license', 'availabilities']"

  describe(
    "returns a 400 Bad Request for errors in the ?aggregations parameter"
  ) {
    it("a single invalid aggregation") {
      assertIsBadRequest(
        s"$rootPath/works?aggregations=foo",
        description =
          s"aggregations: 'foo' is not a valid value. Please choose one of: $aggregationsString"
      )
    }

    it("multiple invalid aggregations") {
      assertIsBadRequest(
        s"$rootPath/works?aggregations=foo,bar",
        description =
          s"aggregations: 'foo', 'bar' are not valid values. Please choose one of: $aggregationsString"
      )
    }

    it("a mixture of valid and invalid aggregations") {
      assertIsBadRequest(
        s"$rootPath/works?aggregations=foo,workType,bar",
        description =
          s"aggregations: 'foo', 'bar' are not valid values. Please choose one of: $aggregationsString"
      )
    }
  }

  it("multiple invalid sorts") {
    assertIsBadRequest(
      s"$rootPath/works?sort=foo,bar",
      description =
        "sort: 'foo', 'bar' are not valid values. Please choose one of: ['production.dates']"
    )
  }

  describe("returns a 400 Bad Request for errors in the ?sort parameter") {
    it("a single invalid sort") {
      assertIsBadRequest(
        s"$rootPath/works?sort=foo",
        description =
          "sort: 'foo' is not a valid value. Please choose one of: ['production.dates']"
      )
    }

    it("multiple invalid sorts") {
      assertIsBadRequest(
        s"$rootPath/works?sort=foo,bar",
        description =
          "sort: 'foo', 'bar' are not valid values. Please choose one of: ['production.dates']"
      )
    }

    it("a mixture of valid and invalid sort") {
      assertIsBadRequest(
        s"$rootPath/works?sort=foo,production.dates,bar",
        description =
          "sort: 'foo', 'bar' are not valid values. Please choose one of: ['production.dates']"
      )
    }
  }

  // This is expected as it's transient parameter that will have valid values changing over time
  // And if there is a client with a deprecated value, we wouldn't want it to fail
  describe("returns a 200 for invalid values in the ?_queryType parameter") {
    it("200s despite being a unknown value") {
      withWorksApi {
        case (_, routes) =>
          assertJsonResponse(
            routes,
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
        assertIsBadRequest(
          s"$rootPath/works?pageSize=$pageSize",
          description = s"pageSize: must be a valid Integer"
        )
      }

      it("just over the maximum") {
        val pageSize = 101
        assertIsBadRequest(
          s"$rootPath/works?pageSize=$pageSize",
          description = "pageSize: must be between 1 and 100"
        )
      }

      it("just below the minimum (zero)") {
        val pageSize = 0
        assertIsBadRequest(
          s"$rootPath/works?pageSize=$pageSize",
          description = "pageSize: must be between 1 and 100"
        )
      }

      it("a large page size") {
        val pageSize = 100000
        assertIsBadRequest(
          s"$rootPath/works?pageSize=$pageSize",
          description = "pageSize: must be between 1 and 100"
        )
      }

      it("a negative page size") {
        val pageSize = -50
        assertIsBadRequest(
          s"$rootPath/works?pageSize=$pageSize",
          description = "pageSize: must be between 1 and 100"
        )
      }
    }

    describe("errors in the ?page query") {
      it("page 0") {
        val page = 0
        assertIsBadRequest(
          s"$rootPath/works?page=$page",
          description = "page: must be greater than 1"
        )
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
        assertIsBadRequest(
          s"$rootPath/works?page=10000",
          description = description
        )
      }

      // https://github.com/wellcometrust/platform/issues/3233
      it("so many pages that a naive (page * pageSize) would overflow") {
        assertIsBadRequest(
          s"$rootPath/works?page=2000000000&pageSize=100",
          description = description
        )
      }

      it("the 101th page with 100 results per page") {
        assertIsBadRequest(
          s"$rootPath/works?page=101&pageSize=100",
          description = description
        )
      }
    }

    it("returns multiple errors if there's more than one invalid parameter") {
      val pageSize = -60
      val page = -50
      assertIsBadRequest(
        s"$rootPath/works?pageSize=$pageSize&page=$page",
        description =
          "page: must be greater than 1, pageSize: must be between 1 and 100"
      )
    }
  }

  describe("returns a 404 Not Found for missing resources") {
    it("looking up a work that doesn't exist") {
      val badId = "doesnotexist"
      assertIsNotFound(
        s"$rootPath/works/$badId",
        description = s"Work not found for identifier $badId"
      )
    }

    it("looking up a work with a malformed identifier") {
      val badId = "zd224ncv]"
      assertIsNotFound(
        s"$rootPath/works/$badId",
        description = s"Work not found for identifier $badId"
      )
    }

    describe("an index that doesn't exist") {
      val indexName = "foobarbaz"

      it("listing") {
        assertIsNotFound(
          s"$rootPath/works?_index=$indexName",
          description = s"There is no index $indexName"
        )
      }

      it("looking up a work") {
        assertIsNotFound(
          s"$rootPath/works/$createCanonicalId?_index=$indexName",
          description = s"There is no index $indexName"
        )
      }

      it("searching") {
        assertIsNotFound(
          s"$rootPath/works/$createCanonicalId?_index=$indexName&query=foobar",
          description = s"There is no index $indexName"
        )
      }
    }
  }

  it("returns an Internal Server error if you try to search a malformed index") {
    // We need to do something that reliably triggers an internal exception
    // in the Elasticsearch handler.
    //
    // By creating an index without a mapping, we don't have a canonicalId field
    // to sort on.  Trying to query this index of these will trigger one such exception!
    withWorksApi {
      case (_, routes) =>
        withEmptyIndex { index =>
          val path = s"$rootPath/works?_index=${index.name}"
          assertJsonResponse(routes, path)(
            Status.InternalServerError ->
              s"""
                 |{
                 |  "@context": "$contextUrl",
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

  def assertIsNotFound(path: String, description: String): Assertion =
    withWorksApi {
      case (_, routes) =>
        assertJsonResponse(routes, path)(
          Status.NotFound ->
            notFound(
              description = description
            )
        )
    }
}