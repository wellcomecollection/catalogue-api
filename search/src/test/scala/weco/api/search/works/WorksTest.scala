package weco.api.search.works

import weco.api.search.generators.PeriodGenerators
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.generators.ItemsGenerators
import weco.catalogue.internal_model.locations._

class WorksTest
    extends ApiWorksTestBase
    with ItemsGenerators
    with PeriodGenerators {
  it("returns a list of works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexFixtures(worksIndex, "list-of-works.0", "list-of-works.1", "list-of-works.2", "list-of-works.3", "list-of-works.4")

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK ->
            """
              |{
              |  "type": "ResultList",
              |  "pageSize": 10,
              |  "totalPages": 1,
              |  "totalResults": 5,
              |  "results": [
              |    {
              |      "id" : "equ81tpb",
              |      "title" : "title-2TWOPFt15v",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type" : "Work"
              |    },
              |    {
              |      "id" : "jtwv1ndp",
              |      "title" : "title-H7sjiP63hv",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "suihraob",
              |      "title" : "title-2RuvBbFbQP",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "vbi1ii19",
              |      "title" : "title-WjGGR8UNWu",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "whfyqts0",
              |      "title" : "title-coJXQqPyux",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    }
              |  ]
              |}
              |""".stripMargin
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
        val work = indexedWork()
          .duration(3600)
          .edition("Special edition")

        insertIntoElasticsearch(worksIndex, work)
        assertJsonResponse(routes, s"$rootPath/works/${work.state.canonicalId}") {
          Status.OK -> s"""
            {
             ${singleWorkResult()},
             "id": "${work.state.canonicalId}",
             "title": "${work.data.title.get}",
             "alternativeTitles": [],
             "availabilities": [${availabilities(work.state.availabilities)}],
             "edition": "Special edition",
             "duration": 3600
            }
            """
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
        assertJsonResponse(routes, s"$rootPath/works?foo=bar") {
          Status.OK -> emptyJsonResult
        }
    }
  }

  it("returns matching results if doing a full-text search") {
    withWorksApi {
      case (worksIndex, routes) =>
        val workDodo = indexedWork().title("A drawing of a dodo")
        val workMouse = indexedWork().title("A mezzotint of a mouse")
        insertIntoElasticsearch(worksIndex, workDodo, workMouse)

        assertJsonResponse(routes, s"$rootPath/works?query=cat") {
          Status.OK -> emptyJsonResult
        }

        assertJsonResponse(routes, s"$rootPath/works?query=dodo") {
          Status.OK -> worksListResponse(works = Seq(workDodo))
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

  it("supports sorting by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        val work1900 = createWorkWithProductionEventFor(year = "1900")
        val work1976 = createWorkWithProductionEventFor(year = "1976")
        val work1904 = createWorkWithProductionEventFor(year = "1904")
        val work2020 = createWorkWithProductionEventFor(year = "2020")
        val work1098 = createWorkWithProductionEventFor(year = "1098")
        insertIntoElasticsearch(
          worksIndex,
          work1900,
          work1976,
          work1904,
          work2020,
          work1098
        )

        assertJsonResponse(routes, s"$rootPath/works?sort=production.dates") {
          Status.OK -> worksListResponse(
            works = Seq(work1098, work1900, work1904, work1976, work2020)
          )
        }
    }
  }

  it("supports sorting of dates in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        val work1900 = createWorkWithProductionEventFor(year = "1900")
        val work1976 = createWorkWithProductionEventFor(year = "1976")
        val work1904 = createWorkWithProductionEventFor(year = "1904")
        insertIntoElasticsearch(worksIndex, work1900, work1976, work1904)

        assertJsonResponse(
          routes,
          s"$rootPath/works?sort=production.dates&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            works = Seq(work1976, work1904, work1900)
          )
        }
    }
  }

  it("returns a tally of work types") {
    withWorksApi {
      case (worksIndex, routes) =>
        val works =
          (1 to 5).map(_ => indexedWork()) ++
            (1 to 3).map(_ => indexedWork().invisible()) ++
            (1 to 2).map(_ => indexedWork().deleted()) ++
            (1 to 4).map(
              _ =>
                indexedWork().redirected(
                  IdState.Identified(
                    canonicalId = createCanonicalId,
                    sourceIdentifier = createSourceIdentifier
                  )
                )
            )

        insertIntoElasticsearch(worksIndex, works: _*)

        assertJsonResponse(routes, s"$rootPath/management/_workTypes") {
          Status.OK ->
            """
              |{
              |  "Visible": 5,
              |  "Invisible": 3,
              |  "Deleted": 2,
              |  "Redirected": 4
              |}
              |""".stripMargin
        }
    }
  }
}
