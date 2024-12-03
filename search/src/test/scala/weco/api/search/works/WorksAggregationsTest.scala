package weco.api.search.works

import org.scalatest.funspec.AnyFunSpec

class WorksAggregationsTest extends AnyFunSpec with ApiWorksTestBase {
  it("aggregates by format") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksFormat: _*)
        val displayWorks = getMinimalDisplayWorks(worksFormat)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=workType"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = worksFormat.size)},
              "aggregations": {
                "type" : "Aggregations",
                "workType": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : {
                        "id" : "a",
                        "label" : "Books"
                      },
                      "count" : ${worksFormatBooks.length},
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "d",
                        "label" : "Journals"
                      },
                      "count" : ${worksFormatJournals.length},
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "i",
                        "label" : "Audio"
                      },
                      "count" : ${worksFormatAudio.length},
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "k",
                        "label" : "Pictures"
                      },
                      "count" : ${worksFormatPictures.length},
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by genre label") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.genres")
        val displayWorks = getMinimalDisplayWorks(ids = Seq("works.genres"))

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=genres.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = 1)},
              "aggregations": {
                "type" : "Aggregations",
                "genres.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : {
                        "id" : "Electronic books",
                        "label" : "Electronic books"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        val works = Seq(
          "work-production.1098",
          "work-production.1904",
          "work-production.2020"
        )

        indexTestDocuments(worksIndex, works: _*)
        val displayWorks = getMinimalDisplayWorks(works)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=production.dates"
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
                        "id": "1098",
                        "label": "1098"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id": "1904",
                        "label": "1904"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id": "2020",
                        "label": "2020"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by language") {
    withWorksApi {
      case (worksIndex, routes) =>
        val ids = Seq(
          "works.languages.0.eng",
          "works.languages.1.eng",
          "works.languages.2.eng",
          "works.languages.3.eng+swe",
          "works.languages.4.eng+swe+tur",
          "works.languages.5.swe",
          "works.languages.6.tur"
        )

        indexTestDocuments(worksIndex, ids: _*)
        val displayWorks = getMinimalDisplayWorks(ids)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=languages"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = 7)},
              "aggregations": {
                "type" : "Aggregations",
                "languages": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : {
                        "id" : "eng",
                        "label" : "English"
                      },
                      "count" : 5,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "swe",
                        "label" : "Swedish"
                      },
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "tur",
                        "label" : "Turkish"
                      },
                      "count" : 2,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by subject label") {
    val works = (0 to 4).map(i => s"works.subjects.$i")

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)
        val displayWorks = getMinimalDisplayWorks(works)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=subjects.label"
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
                      "data" : {
                        "id" : "realAnalysis",
                        "label" : "realAnalysis"
                      },
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "paleoNeuroBiology",
                        "label" : "paleoNeuroBiology"
                      },
                      "count" : 2,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by contributor agent label") {
    val works = (0 to 3).map(i => s"works.contributor.$i")

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)
        val displayWorks = getMinimalDisplayWorks(works)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=contributors.agent.label"
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
                      "data" : {
                        "id" : "47",
                        "label" : "47"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "MI5",
                        "label" : "MI5"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "007",
                        "label" : "007"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "GCHQ",
                        "label" : "GCHQ"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by item license") {
    withWorksApi {
      case (worksIndex, routes) =>
        val works = (0 to 4).map(i => s"works.items-with-licenses.$i")

        indexTestDocuments(worksIndex, works: _*)
        val displayWorks = getMinimalDisplayWorks(works)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=items.locations.license"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = 5)},
              "aggregations": {
                "type" : "Aggregations",
                "items.locations.license": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 3,
                      "data" : {
                        "id" : "cc-by",
                        "label" : "Attribution 4.0 International (CC BY 4.0)"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "cc-by-nc",
                        "label" : "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by availability") {
    withWorksApi {
      case (worksIndex, routes) =>
        val worksAvailabilities = Seq(
          "works.examples.availabilities.open-only",
          "works.examples.availabilities.closed-only",
          "works.examples.availabilities.online-only",
          "works.examples.availabilities.everywhere",
          "works.examples.availabilities.nowhere"
        )

        indexTestDocuments(worksIndex, worksAvailabilities: _*)
        val displayWorks = getMinimalDisplayWorks(worksAvailabilities)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=availabilities"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = worksAvailabilities.size)},
              "aggregations": {
                "availabilities": {
                  "buckets": [
                    {
                      "count": 2,
                      "data": {
                        "label": "Closed stores",
                        "id": "closed-stores"
                      },
                      "type": "AggregationBucket"
                    },
                    {
                    "count" : 2,
                    "data" : {
                      "id" : "online",
                      "label" : "Online"
                    },
                    "type" : "AggregationBucket"
                  },
                    {
                      "count": 2,
                      "data": {
                        "label": "Open shelves",
                        "id": "open-shelves"
                      },
                      "type": "AggregationBucket"
                    }
                  ],
                  "type": "Aggregation"
                },
                "type": "Aggregations"
              },
              "results": [
                ${displayWorks.mkString(",")}
              ]
            }
          """.stripMargin
        }
    }
  }
}
