package weco.api.search.works

import weco.api.search.generators.PeriodGenerators
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work._
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  ProductionEventGenerators
}

import java.time.{LocalDate, Month}

class WorksAggregationsTest
    extends ApiWorksTestBase
    with ItemsGenerators
    with ProductionEventGenerators
    with PeriodGenerators {

  it("aggregates by format") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, formatWorks: _*)

        assertJsonResponse(routes, s"$rootPath/works?aggregations=workType&pageSize=1") {
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

  it("supports aggregating on dates by from year") {
    withWorksApi {
      case (worksIndex, routes) =>
        val periods = List(
          Period(
            id = IdState.Unidentifiable,
            label = "1st May 1970",
            range = Some(
              InstantRange(
                from = LocalDate.of(1970, Month.MAY, 1),
                to = LocalDate.of(1970, Month.MAY, 1),
                label = "1st May 1970"
              )
            )
          ),
          createPeriodForYear("1970"),
          createPeriodForYear("1976"),
          createPeriodForYearRange("1970", "1979")
        )

        val works = periods
          .map { p =>
            indexedWork()
              .production(
                List(createProductionEvent.copy(dates = List(p)))
              )
          }
          .sortBy { _.state.canonicalId }

        insertIntoElasticsearch(worksIndex, works: _*)
        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=production.dates"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
              "aggregations": {
                "type" : "Aggregations",
                "production.dates": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : {
                        "label": "1970",
                        "type": "Period"
                      },
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "label": "1976",
                        "type": "Period"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${works.map(workResponse).mkString(",")}]
            }
          """
        }
    }
  }

  it("aggregates by language") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexExampleDocuments(worksIndex, languageWorks: _*)

        assertJsonResponse(routes, s"$rootPath/works?aggregations=languages&pageSize=1") {
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

  it("supports aggregating on subjects.label, ordered by frequency") {
    val paleoNeuroBiology = createSubjectWith(label = "paleoNeuroBiology")
    val realAnalysis = createSubjectWith(label = "realAnalysis")

    val subjectLists = List(
      List(paleoNeuroBiology),
      List(realAnalysis),
      List(realAnalysis),
      List(paleoNeuroBiology, realAnalysis),
      List.empty
    )

    val works = subjectLists
      .map { indexedWork().subjects(_) }

    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, works: _*)
        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=subjects.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
              "aggregations": {
                "type" : "Aggregations",
                "subjects.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : ${subject(realAnalysis, showConcepts = false)},
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : ${subject(
            paleoNeuroBiology,
            showConcepts = false
          )},
                      "count" : 2,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${works
            .sortBy { _.state.canonicalId }
            .map(workResponse)
            .mkString(",")}]
            }
          """
        }
    }
  }

  it("supports aggregating on contributor agent labels") {
    val agent47 = Agent("47")
    val jamesBond = Agent("007")
    val mi5 = Organisation("MI5")
    val gchq = Organisation("GCHQ")

    val works =
      List(List(agent47), List(agent47), List(jamesBond, mi5), List(mi5, gchq))
        .map { agents =>
          indexedWork().contributors(agents.map(Contributor(_, roles = Nil)))
        }

    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, works: _*)
        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=contributors.agent.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
              "aggregations": {
                "type" : "Aggregations",
                "contributors.agent.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 2,
                      "data" : ${abstractAgent(agent47)},
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : ${abstractAgent(mi5)},
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : ${abstractAgent(jamesBond)},
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : ${abstractAgent(gchq)},
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${works
            .sortBy(_.state.canonicalId)
            .map(workResponse)
            .mkString(",")}]
            }
          """
        }
    }
  }

  it("does not bring down the API when unknown contributor type") {

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
          "works.items-with-licenses.0", "works.items-with-licenses.1", "works.items-with-licenses.2", "works.items-with-licenses.3", "works.items-with-licenses.4"
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
