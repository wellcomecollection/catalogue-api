package weco.api.search.works

import weco.catalogue.internal_model.Implicits._

class WorksAggregationsTest extends ApiWorksTestBase {
  it("aggregates by format") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, formatWorks: _*)

        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=workType&pageSize=1"
        ) {
          Status.OK ->
            s"""
              |{
              |  "aggregations": {
              |    "type" : "Aggregations",
              |    "workType": {
              |      "type" : "Aggregation",
              |      "buckets": [
              |        {
              |          "data" : {
              |            "id" : "a",
              |            "label" : "Books",
              |            "type" : "Format"
              |          },
              |          "count" : 4,
              |          "type" : "AggregationBucket"
              |        },
              |        {
              |          "data" : {
              |            "id" : "d",
              |            "label" : "Journals",
              |            "type" : "Format"
              |          },
              |          "count" : 3,
              |          "type" : "AggregationBucket"
              |        },
              |        {
              |          "data" : {
              |            "id" : "i",
              |            "label" : "Audio",
              |            "type" : "Format"
              |          },
              |          "count" : 2,
              |          "type" : "AggregationBucket"
              |        },
              |        {
              |          "data" : {
              |            "id" : "k",
              |            "label" : "Pictures",
              |            "type" : "Format"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        }
              |      ]
              |    }
              |  },
              |  "type": "ResultList",
              |  "pageSize": 1,
              |  "totalPages": 10,
              |  "totalResults": 10,
              |  "nextPage" : "$publicRootUri/works?aggregations=workType&pageSize=1&page=2",
              |  "results": [
              |    {
              |      "id" : "30wpoiv0",
              |      "title" : "A work with format Journals",
              |      "alternativeTitles" : [],
              |      "availabilities" : [],
              |      "workType" : {
              |        "id" : "d",
              |        "label" : "Journals",
              |        "type" : "Format"
              |      },
              |      "type" : "Work"
              |    }
              |  ]
              |}
              |""".stripMargin
        }
    }
  }

  it("aggregates by concept label on genres") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, "works.genres")

        assertJsonResponse(routes, s"$rootPath/works?aggregations=genres.label") {
          Status.OK ->
            """
              |{
              |  "aggregations": {
              |    "type" : "Aggregations",
              |    "genres.label": {
              |      "type" : "Aggregation",
              |      "buckets": [
              |        {
              |          "data" : {
              |            "label" : "Conceptual Conversations",
              |            "concepts": [],
              |            "type" : "Genre"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        },
              |               {
              |          "data" : {
              |            "label" : "Past Prehistory",
              |            "concepts": [],
              |            "type" : "Genre"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        },
              |               {
              |          "data" : {
              |            "label" : "Pleasant Paris",
              |            "concepts": [],
              |            "type" : "Genre"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        }
              |      ]
              |    }
              |  },
              |  "type": "ResultList",
              |  "pageSize": 10,
              |  "totalPages": 1,
              |  "totalResults": 1,
              |  "results": [
              |    {
              |      "id" : "vsf3c3yc",
              |      "title" : "A work with different concepts in the genre",
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

  it("aggregates by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(
          worksIndex,
          "work-production.1098",
          "work-production.1900",
          "work-production.1904"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=production.dates&pageSize=1"
        ) {
          Status.OK -> s"""
              |{
              |  "aggregations": {
              |    "type" : "Aggregations",
              |    "production.dates": {
              |      "type" : "Aggregation",
              |      "buckets": [
              |        {
              |          "data" : {
              |            "label" : "1098",
              |            "type" : "Period"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        },
              |               {
              |          "data" : {
              |            "label" : "1900",
              |            "type" : "Period"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        },
              |        {
              |          "data" : {
              |            "label" : "1904",
              |            "type" : "Period"
              |          },
              |          "count" : 1,
              |          "type" : "AggregationBucket"
              |        }
              |      ]
              |    }
              |  },
              |  "type": "ResultList",
              |  "pageSize": 1,
              |  "totalPages": 3,
              |  "totalResults": 3,
              |  "nextPage" : "$publicRootUri/works?aggregations=production.dates&pageSize=1&page=2",
              |  "results": [
              |    {
              |      "id" : "3twsgdza",
              |      "title" : "Production event in 1900",
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

  it("aggregates by language") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, languageWorks: _*)

        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=languages&pageSize=1"
        ) {
          Status.OK ->
            s"""
              |{
              |  "aggregations": {
              |    "type" : "Aggregations",
              |    "languages": {
              |      "type" : "Aggregation",
              |      "buckets": [
              |        {
              |          "data" : {
              |            "id": "eng",
              |            "label": "English",
              |            "type": "Language"
              |          },
              |          "count" : 5,
              |          "type" : "AggregationBucket"
              |        },
              |        {
              |          "data" : {
              |            "id": "swe",
              |            "label": "Swedish",
              |            "type": "Language"
              |          },
              |          "count" : 3,
              |          "type" : "AggregationBucket"
              |        },
              |        {
              |          "data" : {
              |            "id": "tur",
              |            "label": "Turkish",
              |            "type": "Language"
              |          },
              |          "count" : 2,
              |          "type" : "AggregationBucket"
              |        }
              |      ]
              |    }
              |  },
              |  "type": "ResultList",
              |  "pageSize": 1,
              |  "totalPages": 7,
              |  "totalResults": 7,
              |  "nextPage" : "$publicRootUri/works?aggregations=languages&pageSize=1&page=2",
              |  "results": [
              |    {
              |      "id" : "6ne7xwrd",
              |      "title" : "A work with languages English",
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

  it("aggregates by subject label") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(
          worksIndex,
          "works.subjects.0",
          "works.subjects.1",
          "works.subjects.2",
          "works.subjects.3",
          "works.subjects.4"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=subjects.label&pageSize=1"
        ) {
          Status.OK ->
            s"""
               |{
               |  "aggregations" : {
               |    "subjects.label" : {
               |      "buckets" : [
               |        {
               |          "count" : 3,
               |          "data" : {
               |            "concepts" : [
               |            ],
               |            "label" : "realAnalysis",
               |            "type" : "Subject"
               |          },
               |          "type" : "AggregationBucket"
               |        },
               |        {
               |          "count" : 2,
               |          "data" : {
               |            "concepts" : [
               |            ],
               |            "label" : "paleoNeuroBiology",
               |            "type" : "Subject"
               |          },
               |          "type" : "AggregationBucket"
               |        }
               |      ],
               |      "type" : "Aggregation"
               |    },
               |    "type" : "Aggregations"
               |  },
               |  "nextPage" : "$publicRootUri/works?aggregations=subjects.label&pageSize=1&page=2",
               |  "pageSize" : 1,
               |  "results" : [
               |    {
               |      "id" : "2eqbmtfc",
               |      "title" : "title-JnlkosWvAm",
               |      "alternativeTitles" : [],
               |      "availabilities" : [],
               |      "type" : "Work"
               |    }
               |  ],
               |  "totalPages" : 5,
               |  "totalResults" : 5,
               |  "type" : "ResultList"
               |}
               |""".stripMargin
        }
    }
  }

  it("aggregates by contributor agent label") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(
          worksIndex,
          "works.contributor.0",
          "works.contributor.1",
          "works.contributor.2",
          "works.contributor.3"
        )

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/works?aggregations=contributors.agent.label&pageSize=1"
        ) {
          Status.OK ->
            s"""
               |{
               |  "aggregations" : {
               |    "contributors.agent.label" : {
               |      "buckets" : [
               |        {
               |          "count" : 2,
               |          "data" : {
               |            "label" : "47",
               |            "type" : "Agent"
               |          },
               |          "type" : "AggregationBucket"
               |        },
               |        {
               |          "count" : 2,
               |          "data" : {
               |            "label" : "MI5",
               |            "type" : "Organisation"
               |          },
               |          "type" : "AggregationBucket"
               |        },
               |        {
               |          "count" : 1,
               |          "data" : {
               |            "label" : "007",
               |            "type" : "Agent"
               |          },
               |          "type" : "AggregationBucket"
               |        },
               |        {
               |          "count" : 1,
               |          "data" : {
               |            "label" : "GCHQ",
               |            "type" : "Organisation"
               |          },
               |          "type" : "AggregationBucket"
               |        }
               |      ],
               |      "type" : "Aggregation"
               |    },
               |    "type" : "Aggregations"
               |  },
               |  "nextPage" : "$publicRootUri/works?aggregations=contributors.agent.label&pageSize=1&page=2",
               |  "pageSize" : 1,
               |  "results" : [
               |    {
               |      "id" : "cwk8hypk",
               |      "title" : "title-D9r1zHmiHw",
               |      "alternativeTitles" : [],
               |      "availabilities" : [],
               |      "type" : "Work"
               |    }
               |  ],
               |  "totalPages" : 4,
               |  "totalResults" : 4,
               |  "type" : "ResultList"
               |}
               |""".stripMargin
        }
    }
  }

  // TODO: This test is very tied to the current index structure, and is moderately
  // fiddly to make work with the new ingestor.
  //
  // It's testing an edge case that should never occur in practice; while we should keep
  // this test, let's revisit it after we finish restructuring the index.
  ignore("does not bring down the API when unknown contributor type") {
    val work = indexedWork()

    val workWithContributor = work.copy(
      state = work.state.copy(
        derivedData = work.state.derivedData.copy(
          contributorAgents = List("Producer:Keith")
        )
      )
    )

    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, workWithContributor)
        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=contributors.agent.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = 1)},
              "aggregations": {
                "type" : "Aggregations",
                "contributors.agent.label": {
                  "type" : "Aggregation",
                  "buckets": []
                }
              },
              "results": [${workResponse(workWithContributor)}]
            }
          """
        }
    }
  }

  it("aggregates by item license") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(
          worksIndex,
          "works.items-with-licenses.0",
          "works.items-with-licenses.1",
          "works.items-with-licenses.2",
          "works.items-with-licenses.3",
          "works.items-with-licenses.4"
        )

        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=items.locations.license&pageSize=1"
        ) {
          Status.OK ->
            s"""
               |{
               |  "aggregations": {
               |    "type" : "Aggregations",
               |    "items.locations.license": {
               |      "type" : "Aggregation",
               |      "buckets": [
               |        {
               |          "count" : 3,
               |          "data" : {
               |            "id" : "cc-by",
               |            "label" : "Attribution 4.0 International (CC BY 4.0)",
               |            "type" : "License",
               |            "url" : "http://creativecommons.org/licenses/by/4.0/"
               |          },
               |          "type" : "AggregationBucket"
               |        },
               |        {
               |          "count" : 2,
               |          "data" : {
               |            "id" : "cc-by-nc",
               |            "label" : "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)",
               |            "type" : "License",
               |            "url" : "https://creativecommons.org/licenses/by-nc/4.0/"
               |          },
               |          "type" : "AggregationBucket"
               |        }
               |      ]
               |    }
               |  },
               |  "type": "ResultList",
               |  "pageSize": 1,
               |  "totalPages": 5,
               |  "totalResults": 5,
               |  "nextPage" : "$publicRootUri/works?aggregations=items.locations.license&pageSize=1&page=2",
               |  "results": [
               |    {
               |      "id" : "454vgiiy",
               |      "title" : "title-ODfcTxxAAI",
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

  it("aggregates by availabilities") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, everythingWorks: _*)

        assertJsonResponse(
          routes = routes,
          path = s"$rootPath/works?aggregations=availabilities&pageSize=1"
        ) {
          Status.OK ->
            s"""
               |{
               |  "aggregations": {
               |    "availabilities": {
               |      "buckets": [
               |        {
               |          "count": 2,
               |          "data": {
               |            "label": "Closed stores",
               |            "id": "closed-stores",
               |            "type" : "Availability"
               |          },
               |          "type": "AggregationBucket"
               |        },
               |        {
               |          "count": 2,
               |          "data": {
               |            "label": "Open shelves",
               |            "id": "open-shelves",
               |            "type" : "Availability"
               |          },
               |          "type": "AggregationBucket"
               |        }
               |      ],
               |      "type": "Aggregation"
               |    },
               |    "type": "Aggregations"
               |  },
               |  "type": "ResultList",
               |  "pageSize": 1,
               |  "totalPages": 3,
               |  "totalResults": 3,
               |  "nextPage" : "$publicRootUri/works?aggregations=availabilities&pageSize=1&page=2",
               |  "results": [
               |    {
               |      "id" : "4ed5mjia",
               |      "title" : "A work with all the include-able fields",
               |      "alternativeTitles" : [],
               |      "availabilities" : [
               |        {
               |          "id" : "open-shelves",
               |          "label" : "Open shelves",
               |          "type" : "Availability"
               |        }
               |      ],
               |      "type" : "Work"
               |    }
               |  ]
               |}
               |""".stripMargin
        }
    }
  }
}
