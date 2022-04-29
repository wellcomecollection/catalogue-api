package weco.api.search.works

class WorksTest extends ApiWorksTestBase {
  it("returns a list of works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, listOfWorks: _*)

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
              |      "id" : "1tpb2two",
              |      "title" : "title-PFt15vaMzx",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type" : "Work"
              |    },
              |    {
              |      "id" : "hfyqts0c",
              |      "title" : "title-oJXQqPyuxb",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "ihraob2r",
              |      "title" : "title-uvBbFbQPNB",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "v1ndph7s",
              |      "title" : "title-jiP63hvGdg",
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
        indexExampleDocuments(worksIndex, "works.visible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/vbi1ii19") {
          Status.OK ->
            """
              |{
              |  "id" : "vbi1ii19",
              |  "title" : "title-WjGGR8UNWu",
              |  "alternativeTitles" : [],
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
        indexExampleDocuments(worksIndex, "work-with-edition-and-duration")

        assertJsonResponse(routes, path = s"$rootPath/works/xckp7yni") {
          Status.OK ->
            """
              |{
              |  "id" : "xckp7yni",
              |  "title" : "title-35xO0fGSrU",
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

  it("returns the right page of results") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, listOfWorks: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?page=1&pageSize=2") {
          Status.OK ->
            s"""
              |{
              |  "type": "ResultList",
              |  "pageSize": 2,
              |  "totalPages": 3,
              |  "totalResults": 5,
              |  "results": [
              |    {
              |      "id" : "1tpb2two",
              |      "title" : "title-PFt15vaMzx",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type" : "Work"
              |    },
              |    {
              |      "id" : "hfyqts0c",
              |      "title" : "title-oJXQqPyuxb",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    }
              |  ],
              |  "nextPage": "$publicRootUri/works?page=2&pageSize=2"
              |}
              |""".stripMargin
        }

        assertJsonResponse(routes, path = s"$rootPath/works?page=2&pageSize=2") {
          Status.OK ->
            s"""
               |{
               |  "type": "ResultList",
               |  "pageSize": 2,
               |  "totalPages": 3,
               |  "totalResults": 5,
               |  "results": [
               |    {
               |      "id" : "ihraob2r",
               |      "title" : "title-uvBbFbQPNB",
               |      "alternativeTitles" : [],
               |      "availabilities": [],
               |      "type": "Work"
               |    },
               |    {
               |      "id" : "v1ndph7s",
               |      "title" : "title-jiP63hvGdg",
               |      "alternativeTitles" : [],
               |      "availabilities": [],
               |      "type": "Work"
               |    }
               |  ],
               |  "nextPage": "$publicRootUri/works?page=3&pageSize=2",
               |  "prevPage": "$publicRootUri/works?page=1&pageSize=2"
               |}
               |""".stripMargin
        }

        assertJsonResponse(routes, path = s"$rootPath/works?page=3&pageSize=2") {
          Status.OK ->
            s"""
               |{
               |  "type": "ResultList",
               |  "pageSize": 2,
               |  "totalPages": 3,
               |  "totalResults": 5,
               |  "results": [
               |    {
               |      "id" : "vbi1ii19",
               |      "title" : "title-WjGGR8UNWu",
               |      "alternativeTitles" : [],
               |      "availabilities": [],
               |      "type": "Work"
               |    }
               |  ],
               |  "prevPage": "$publicRootUri/works?page=2&pageSize=2"
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
        indexExampleDocuments(worksIndex, "work-title-dodo", "work-title-mouse")

        assertJsonResponse(routes, path = s"$rootPath/works?query=cat") {
          Status.OK -> emptyJsonResult
        }

        assertJsonResponse(routes, path = s"$rootPath/works?query=dodo") {
          Status.OK ->
            """
              |{
              |  "type": "ResultList",
              |  "pageSize": 10,
              |  "totalPages": 1,
              |  "totalResults": 1,
              |  "results": [
              |    {
              |      "id" : "xrtrirrr",
              |      "title" : "A drawing of a dodo",
              |      "alternativeTitles" : [],
              |      "availabilities" : [],
              |      "type" : "Work"
              |    }
              |  ]
              |}
              |""".stripMargin
        }
    }
  }

  it("shows the thumbnail field if available") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, "work-thumbnail")

        assertJsonResponse(routes, path = s"$rootPath/works/zgalieza") {
          Status.OK ->
            """
              |{
              |  "id" : "zgalieza",
              |  "title" : "title-giK4BVNTn8",
              |  "alternativeTitles" : [],
              |  "availabilities" : [],
              |  "thumbnail" : {
              |    "locationType" : {
              |      "id" : "iiif-presentation",
              |      "label" : "IIIF Presentation API",
              |      "type" : "LocationType"
              |    },
              |    "url" : "https://iiif.wellcomecollection.org/image/DWb.jpg/info.json",
              |    "linkText" : "Link text: 8mq7LN",
              |    "license" : {
              |      "id" : "cc-by",
              |      "label" : "Attribution 4.0 International (CC BY 4.0)",
              |      "url" : "http://creativecommons.org/licenses/by/4.0/",
              |      "type" : "License"
              |    },
              |    "accessConditions" : [],
              |    "type" : "DigitalLocation"
              |  },
              |  "type" : "Work"
              |}
              |""".stripMargin
        }
    }
  }

  val datedDocuments = List(
    "work-production.1098",
    "work-production.1900",
    "work-production.1904",
    "work-production.1976",
    "work-production.2020"
  )

  it("supports sorting by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, datedDocuments: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates"
        ) {
          Status.OK ->
            """
              |{
              |  "type": "ResultList",
              |  "pageSize": 10,
              |  "totalPages": 1,
              |  "totalResults": 5,
              |  "results": [
              |    {
              |      "id" : "rz9qpj8a",
              |      "title" : "Production event in 1098",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type" : "Work"
              |    },
              |    {
              |      "id" : "3twsgdza",
              |      "title" : "Production event in 1900",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "udfit1sh",
              |      "title" : "Production event in 1904",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "yzum5kot",
              |      "title" : "Production event in 1976",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "pr8zhydf",
              |      "title" : "Production event in 2020",
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

  it("supports sorting by production date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, datedDocuments: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates&sortOrder=desc"
        ) {
          Status.OK ->
            """
              |{
              |  "type": "ResultList",
              |  "pageSize": 10,
              |  "totalPages": 1,
              |  "totalResults": 5,
              |  "results": [
              |    {
              |      "id" : "pr8zhydf",
              |      "title" : "Production event in 2020",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "yzum5kot",
              |      "title" : "Production event in 1976",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "udfit1sh",
              |      "title" : "Production event in 1904",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "3twsgdza",
              |      "title" : "Production event in 1900",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type": "Work"
              |    },
              |    {
              |      "id" : "rz9qpj8a",
              |      "title" : "Production event in 1098",
              |      "alternativeTitles" : [],
              |      "availabilities": [],
              |      "type" : "Work"
              |    }
              |  ]
              |}
              |""".stripMargin
        }
    }
  }

  it("returns a tally of work types") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, listOfWorks: _*)

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
