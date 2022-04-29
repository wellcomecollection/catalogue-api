package weco.api.search.works

import weco.api.search.models.request.WorksIncludes

class WorksTest extends ApiWorksTestBase {
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
        indexTestDocuments(worksIndex, visibleWorks.head)

        assertJsonResponse(routes, path = s"$rootPath/works/7sjip63h") {
          Status.OK ->
            """
              |{
              |  "id": "7sjip63h",
              |  "title": "title-dgZfc8BAUa",
              |  "alternativeTitles": [],
              |  "availabilities": [],
              |  "type": "Work"
              |}
              |""".stripMargin
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
              |  "id": "kgxrtrir",
              |  "title": "title-YGQYiF7SAh",
              |  "alternativeTitles": [],
              |  "availabilities": [],
              |  "edition": "Special edition",
              |  "duration": 3600,
              |  "type": "Work"
              |}
              |""".stripMargin
        }
    }
  }

  it("returns the requested page of results") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, visibleWorks: _*)

        assertJsonResponse(routes, s"$rootPath/works?page=2&pageSize=1") {
          Status.OK ->
            s"""
               |{
               |  "results": [
               |    ${getTestWork("works.visible.1").display.withIncludes(WorksIncludes.none).noSpaces}
               |  ],
               |  "nextPage" : "$publicRootUri/works?page=3&pageSize=1",
               |  "prevPage" : "$publicRootUri/works?page=1&pageSize=1",
               |  "type": "ResultList",
               |}
               |""".stripMargin
        }

        assertJsonResponse(routes, s"$rootPath/works?page=3&pageSize=1") {
          Status.OK ->
            s"""
               |{
               |  "results": [
               |    ${getTestWork("works.visible.2").display.withIncludes(WorksIncludes.none).noSpaces}
               |  ],
               |  "nextPage" : "$publicRootUri/works?page=4&pageSize=1",
               |  "prevPage" : "$publicRootUri/works?page=2&pageSize=1",
               |  "type": "ResultList",
               |}
               |""".stripMargin
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
        indexTestDocuments(worksIndex, "work-thumbnail")

        assertJsonResponse(routes, path = s"$rootPath/works/b2tsq547") {
          Status.OK ->
            """
              |{
              |  "id": "b2tsq547",
              |  "title" : "title-ZNHa9f6J8i",
              |  "alternativeTitles": [],
              |  "availabilities": [],
              |  "thumbnail" : {
              |    "locationType" : {
              |      "id" : "iiif-presentation",
              |      "label" : "IIIF Presentation API",
              |      "type" : "LocationType"
              |    },
              |    "url" : "https://iiif.wellcomecollection.org/image/VKc.jpg/info.json",
              |    "credit" : "Credit line: xg9Ouz",
              |    "linkText" : "Link text: lieZAgiK4B",
              |    "license" : {
              |      "id" : "cc-by",
              |      "label" : "Attribution 4.0 International (CC BY 4.0)",
              |      "url" : "http://creativecommons.org/licenses/by/4.0/",
              |      "type" : "License"
              |    },
              |    "accessConditions" : [
              |    ],
              |    "type" : "DigitalLocation"
              |  },
              |  "type": "Work"
              |}
              |""".stripMargin
        }
    }
  }

  it("sorts by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        val works = Seq(
          "work-production.1098",
          "work-production.1900",
          "work-production.1904",
          "work-production.1976",
          "work-production.2020"
        )

        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?sort=production.dates") {
          Status.OK -> newWorksListResponse(
            ids = works.sorted,
            sortByCanonicalId = false
          )
        }
    }
  }

  it("sorts by production date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        val works = Seq(
          "work-production.1098",
          "work-production.1900",
          "work-production.1904",
          "work-production.1976",
          "work-production.2020"
        )

        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates&sortOrder=desc"
        ) {
          Status.OK -> newWorksListResponse(
            ids = works.sorted.reverse,
            sortByCanonicalId = false
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
            """
              |{
              |  "Visible": 5,
              |  "Invisible": 3,
              |  "Deleted": 4,
              |  "Redirected": 2
              |}
              |""".stripMargin
        }
    }
  }
}
