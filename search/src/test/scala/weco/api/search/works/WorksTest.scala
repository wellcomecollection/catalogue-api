package weco.api.search.works

class WorksTest extends ApiWorksTestBase {
  it("returns a list of works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestWorks(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> worksListResponse(visibleWorks)
        }
    }
  }

  it("returns a single work when requested with id") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestWorks(worksIndex, "works.visible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/7sjip63h") {
          Status.OK -> s"""
            {
             "id": "7sjip63h",
             "title": "title-dgZfc8BAUa",
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
        indexTestWorks(worksIndex, "work-with-edition-and-duration")

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
        indexTestWorks(worksIndex, visibleWorks: _*)

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
                {
                 "id": "7sjip63h",
                 "title": "title-dgZfc8BAUa",
                 "alternativeTitles": [],
                 "availabilities": [],
                 "type": "Work"
                },
                {
                 "id": "ob2ruvbb",
                 "title": "title-QPNB7ZuKSW",
                 "alternativeTitles": [],
                 "availabilities": [],
                 "type": "Work"
                }
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
                {
                 "id": "pft15vam",
                 "title": "title-d8prf0oY4u",
                 "alternativeTitles": [],
                 "availabilities": [],
                 "type": "Work"
                },
                {
                 "id": "vbi1ii19",
                 "title": "title-GGR8UNWutF",
                 "alternativeTitles": [],
                 "availabilities": [],
                 "type": "Work"
                }
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
                {
                 "id": "yqts0coj",
                 "title": "title-qPyuxbr589",
                 "alternativeTitles": [],
                 "availabilities": [],
                 "type": "Work"
                }
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
        indexTestWorks(worksIndex, "work-title-dodo", "work-title-mouse")

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
        indexTestWorks(worksIndex, "work-thumbnail")

        assertJsonResponse(routes, path = s"$rootPath/works/b2tsq547") {
          Status.OK -> s"""
            {
              "id" : "b2tsq547",
              "title" : "title-ZNHa9f6J8i",
              "alternativeTitles" : [],
              "thumbnail" : {
                "locationType" : {
                  "id" : "iiif-presentation",
                  "label" : "IIIF Presentation API",
                  "type" : "LocationType"
                },
                "url" : "https://iiif.wellcomecollection.org/image/VKc.jpg/info.json",
                "credit" : "Credit line: xg9Ouz",
                "linkText" : "Link text: lieZAgiK4B",
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
        indexTestWorks(
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
        indexTestWorks(
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

  it("returns a tally of work types") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestWorks(worksIndex, works: _*)

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
