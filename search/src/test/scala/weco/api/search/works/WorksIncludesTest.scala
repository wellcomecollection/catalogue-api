package weco.api.search.works

import org.scalatest.funspec.AnyFunSpec

class WorksIncludesTest extends AnyFunSpec with ApiWorksTestBase {
  describe("identifiers includes") {
    it(
      "includes a list of identifiers on a list endpoint if we pass ?include=identifiers"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=identifiers"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-list-identifiers.json"
            )
          }
      }
    }

    it(
      "includes a list of identifiers on a single work endpoint if we pass ?include=identifiers"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=identifiers"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-identifiers.json"
            )
          }
      }
    }
  }

  it("renders the items if the items include is present") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works/i4c7c9yl?include=items"
        ) {
          Status.OK -> readResource(
            "expected_responses/works-include-single-items.json"
          )
        }
    }
  }

  describe("subject includes") {
    it(
      "includes a list of subjects on a list endpoint if we pass ?include=subjects"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=subjects") {
            Status.OK -> readResource(
              "expected_responses/works-include-list-subjects.json"
            )
          }
      }
    }

    it(
      "includes a list of subjects on a single work endpoint if we pass ?include=subjects"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=subjects"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-subjects.json"
            )
          }
      }
    }
  }

  describe("genre includes") {
    it(
      "includes a list of genres on a list endpoint if we pass ?include=genres"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=genres") {
            Status.OK -> readResource(
              "expected_responses/works-include-list-genres.json"
            )
          }
      }
    }

    it(
      "includes a list of genres on a single work endpoint if we pass ?include=genres"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=genres"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-genres.json"
            )
          }
      }
    }
  }

  describe("contributor includes") {
    it(
      "includes a list of contributors on a list endpoint if we pass ?include=contributors"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=contributors"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-list-contributors.json"
            )
          }
      }
    }

    it(
      "includes a list of contributors on a single work endpoint if we pass ?include=contributors"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=contributors"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-contributors.json"
            )
          }
      }
    }
  }

  describe("production includes") {
    it(
      "includes a list of production events on a list endpoint if we pass ?include=production"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=production"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-list-production.json"
            )
          }
      }
    }

    it(
      "includes a list of production on a single work endpoint if we pass ?include=production"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=production"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-production.json"
            )
          }
      }
    }
  }

  describe("languages includes") {
    it("includes languages on a list endpoint if we pass ?include=languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=languages"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-list-languages.json"
            )
          }
      }
    }

    it("includes languages on a work endpoint if we pass ?include=languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=languages"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-languages.json"
            )
          }
      }
    }
  }

  describe("notes includes") {
    it("includes notes on the list endpoint if we pass ?include=notes") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=notes") {
            Status.OK -> readResource(
              "expected_responses/works-include-list-notes.json"
            )
          }
      }
    }

    it("includes notes on the single work endpoint if we pass ?include=notes") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=notes"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-notes.json"
            )
          }
      }
    }
  }

  it("includes formerFrequency if we pass ?include=formerFrequency") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?include=formerFrequency"
        ) {
          Status.OK -> readResource(
            "expected_responses/works-include-list-formerFrequency.json"
          )
        }
    }
  }

  it("includes designation if we pass ?include=designation") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?include=designation"
        ) {
          Status.OK -> readResource(
            "expected_responses/works-include-list-designation.json"
          )
        }
    }
  }

  describe("image includes") {
    it(
      "includes a list of images on the list endpoint if we pass ?include=images"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=images") {
            Status.OK -> readResource(
              "expected_responses/works-include-list-images.json"
            )
          }
      }
    }

    it(
      "includes a list of images on a single work endpoint if we pass ?include=images"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=images"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-images.json"
            )
          }
      }
    }
  }

  describe("relation includes") {
    it("includes parts") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=parts"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-parts.json"
            )
          }
      }
    }

    it("includes partOf") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=partOf"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-partOf.json"
            )
          }
      }
    }
  }

  describe("holdings includes") {
    it("on the list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=holdings") {
            Status.OK -> readResource(
              "expected_responses/works-include-list-holdings.json"
            )
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/i4c7c9yl?include=holdings"
          ) {
            Status.OK -> readResource(
              "expected_responses/works-include-single-holdings.json"
            )
          }
      }
    }
  }
}
