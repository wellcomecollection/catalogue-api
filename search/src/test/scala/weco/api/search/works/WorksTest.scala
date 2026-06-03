package weco.api.search.works

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.models.request.WorksIncludes

class WorksTest extends AnyFunSpec with ApiWorksTestBase {
  it("returns a list of works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> worksListResponse(visibleWorks)
        }
    }
  }

  it("returns a single work when requested with id") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.visible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/afza5vpu") {
          Status.OK -> s"""
            {
              "id" : "afza5vpu",
              "title" : "52RvEJgwBuNO6n9",
              "alternativeTitles": [],
              "availabilities": [],
              "type": "Work"
            }
          """
        }
    }
  }

  it("returns optional fields when they exist") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-with-edition-and-duration")

        assertJsonResponse(routes, path = s"$rootPath/works/mwheb72m") {
          Status.OK ->
            """
              |{
              |  "id" : "mwheb72m",
              |  "title" : "dyagzJHsC6FYLTR",
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
        indexTestDocuments(worksIndex, visibleWorks: _*)

        val totalPages = math.ceil(visibleWorks.length / 2.0).toInt

        assertJsonResponse(routes, path = s"$rootPath/works?page=1&pageSize=2") {
          Status.OK -> s"""
            {
              "type": "ResultList",
              "pageSize": 2,
              "totalPages": $totalPages,
              "totalResults": ${visibleWorks.length},
              "nextPage": "$publicRootUri/works?page=2&pageSize=2",
              "results": [
                ${getVisibleWork("works.visible.0").display
            .withIncludes(WorksIncludes.none)},
                ${getVisibleWork("works.visible.1").display
            .withIncludes(WorksIncludes.none)}
              ]
            }
          """
        }

        assertJsonResponse(routes, path = s"$rootPath/works?page=2&pageSize=2") {
          Status.OK -> s"""
            {
              "type": "ResultList",
              "pageSize": 2,
              "totalPages": $totalPages,
              "totalResults": ${visibleWorks.length},
              "prevPage": "$publicRootUri/works?page=1&pageSize=2",
              "nextPage": "$publicRootUri/works?page=3&pageSize=2",
              "results": [
                ${getVisibleWork("works.visible.2").display
            .withIncludes(WorksIncludes.none)},
                ${getVisibleWork("works.visible.3").display
            .withIncludes(WorksIncludes.none)}
              ]
            }
          """
        }

        assertJsonResponse(routes, path = s"$rootPath/works?page=3&pageSize=2") {
          Status.OK -> s"""
            {
              "type": "ResultList",
              "pageSize": 2,
              "totalPages": $totalPages,
              "totalResults": ${visibleWorks.length},
              "prevPage": "$publicRootUri/works?page=2&pageSize=2",
              "results": [
                ${getVisibleWork("works.visible.4").display
            .withIncludes(WorksIncludes.none)}
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
          Status.OK -> worksListResponse(ids = Seq("work-title-dodo"))
        }
    }
  }

  it("shows the thumbnail field if available") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-thumbnail")

        assertJsonResponse(routes, path = s"$rootPath/works/oavuuc7j") {
          Status.OK -> """
            {
              "id" : "oavuuc7j",
              "title" : "gMFjKzORB4HFR64",
              "alternativeTitles" : [],
              "thumbnail" : {
                "locationType" : {
                  "id" : "iiif-presentation",
                  "label" : "IIIF Presentation API",
                  "type" : "LocationType"
                },
                "url" : "https://iiif.wellcomecollection.org/image/hFz.jpg/info.json",
                "license" : {
                  "id" : "cc-by",
                  "label" : "Attribution 4.0 International (CC BY 4.0)",
                  "url" : "http://creativecommons.org/licenses/by/4.0/",
                  "type" : "License"
                },
                "accessConditions" : [
                ],
                "type" : "DigitalLocation"
              },
              "availabilities" : [],
              "type" : "Work"
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
          "work-production.1098",
          "work-production.1900",
          "work-production.1904"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-production.1098",
              "work-production.1900",
              "work-production.1904"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-production.1098",
          "work-production.1900",
          "work-production.1904"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-production.1904",
              "work-production.1900",
              "work-production.1098"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by digital location created date") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-digital-location.2020",
          "work-digital-location.2022",
          "work-digital-location.2021"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=items.locations.createdDate"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-digital-location.2020",
              "work-digital-location.2021",
              "work-digital-location.2022"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by digital location created date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-digital-location.2020",
          "work-digital-location.2022",
          "work-digital-location.2021"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=items.locations.createdDate&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-digital-location.2022",
              "work-digital-location.2021",
              "work-digital-location.2020"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("returns documents whose digital location has no createdDate last") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-digital-location.2020",
          "work-digital-location.no-date",
          "work-digital-location.2021"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=items.locations.createdDate"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-digital-location.2020",
              "work-digital-location.2021",
              "work-digital-location.no-date"
            ),
            strictOrdering = true
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
