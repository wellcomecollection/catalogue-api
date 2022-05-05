package weco.api.search.works

import weco.api.search.fixtures.TestDocumentFixtures
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.locations._
import weco.catalogue.internal_model.work.generators.ItemsGenerators

class WorksTest
    extends ApiWorksTestBase
    with TestDocumentFixtures
    with ItemsGenerators {
  it("returns a list of works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> newWorksListResponse(visibleWorks)
        }
    }
  }

  it("returns a single work when requested with id") {
    withWorksApi {
      case (worksIndex, routes) =>
        val work = indexedWork()

        insertIntoElasticsearch(worksIndex, work)

        assertJsonResponse(routes, s"$rootPath/works/${work.state.canonicalId}") {
          Status.OK -> s"""
            {
             ${singleWorkResult()},
             "id": "${work.state.canonicalId}",
             "title": "${work.data.title.get}",
             "alternativeTitles": [],
             "availabilities": [${availabilities(work.state.availabilities)}]
            }
          """
        }
    }
  }

  it("returns optional fields when they exist") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-with-edition-and-duration")

        assertJsonResponse(routes, path = s"$rootPath/works/kgxrtrir") {
          Status.OK ->
            """
              |{
              |  "id" : "kgxrtrir",
              |  "title" : "title-YGQYiF7SAh",
              |  "alternativeTitles" : [],
              |  "availabilities" : [],
              |  "edition" : "Special edition",
              |  "duration" : 3600,
              |  "type" : "Work"
              |}
              |""".stripMargin
        }
    }
  }

  it(
    "returns the requested page of results when requested with page & pageSize"
  ) {
    withWorksApi {
      case (worksIndex, routes) =>
        val works = indexedWorks(count = 3).sortBy {
          _.state.canonicalId
        }

        insertIntoElasticsearch(worksIndex, works: _*)

        assertJsonResponse(routes, s"$rootPath/works?page=2&pageSize=1") {
          Status.OK -> s"""
            {
              ${resultList(pageSize = 1, totalPages = 3, totalResults = 3)},
              "prevPage": "$publicRootUri/works?page=1&pageSize=1",
              "nextPage": "$publicRootUri/works?page=3&pageSize=1",
              "results": [
                ${workResponse(works(1))}
              ]
            }
          """
        }

        assertJsonResponse(routes, s"$rootPath/works?page=1&pageSize=1") {
          Status.OK -> s"""
            {
              ${resultList(pageSize = 1, totalPages = 3, totalResults = 3)},
              "nextPage": "$publicRootUri/works?page=2&pageSize=1",
              "results": [
                ${workResponse(works(0))}
              ]
            }
          """
        }

        assertJsonResponse(routes, s"$rootPath/works?page=3&pageSize=1") {
          Status.OK -> s"""
            {
              ${resultList(pageSize = 1, totalPages = 3, totalResults = 3)},
              "prevPage": "$publicRootUri/works?page=2&pageSize=1",
              "results": [
                ${workResponse(works(2))}
              ]
            }
          """
        }
    }
  }

  it("ignores parameters that are unused when making an API request") {
    withWorksApi {
      case (_, routes) =>
        assertJsonResponse(routes, path = s"$rootPath/works?foo=bar") {
          Status.OK -> emptyJsonResult
        }
    }
  }

  it("returns matching results if doing a full-text search") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-title-dodo", "work-title-mouse")

        assertJsonResponse(routes, s"$rootPath/works?query=cat") {
          Status.OK -> emptyJsonResult
        }

        assertJsonResponse(routes, s"$rootPath/works?query=dodo") {
          Status.OK -> newWorksListResponse(ids = Seq("work-title-dodo"))
        }
    }
  }

  it("shows the thumbnail field if available") {
    withWorksApi {
      case (worksIndex, routes) =>
        val thumbnailLocation = DigitalLocation(
          locationType = LocationType.ThumbnailImage,
          url = "https://iiif.example.org/1234/default.jpg",
          license = Some(License.CCBY)
        )
        val work = indexedWork()
          .thumbnail(thumbnailLocation)
          .items(
            List(createIdentifiedItemWith(locations = List(thumbnailLocation)))
          )
        insertIntoElasticsearch(worksIndex, work)

        assertJsonResponse(routes, s"$rootPath/works") {
          Status.OK -> s"""
            {
              ${resultList(totalResults = 1)},
              "results": [
               {
                 "type": "Work",
                 "id": "${work.state.canonicalId}",
                 "title": "${work.data.title.get}",
                 "alternativeTitles": [],
                 "availabilities": [${availabilities(work.state.availabilities)}],
                 "thumbnail": ${location(work.data.thumbnail.get)}
                }
              ]
            }
          """
        }
    }
  }

  it("sorts by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-production.1098", "work-production.1900", "work-production.1904",
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("work-production.1098", "work-production.1900", "work-production.1904")
          )
        }
    }
  }

  it("sorts by date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-production.1098", "work-production.1900", "work-production.1904",
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates&sortOrder=desc"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("work-production.1904", "work-production.1900", "work-production.1098")
          )
        }
    }
  }

  it("returns a tally of work types") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/management/_workTypes") {
          Status.OK ->
            s"""
              |{
              |  "Visible": ${visibleWorks.length},
              |  "Invisible": ${invisibleWorks.length},
              |  "Deleted": ${deletedWorks.length},
              |  "Redirected": ${redirectedWorks.length}
              |}
              |""".stripMargin
        }
    }
  }
}
