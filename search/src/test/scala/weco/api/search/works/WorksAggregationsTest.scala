package weco.api.search.works

class WorksAggregationsTest extends ApiWorksTestBase {
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
                        "label" : "Books",
                        "type" : "Format"
                      },
                      "count" : ${worksFormatBooks.length},
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "d",
                        "label" : "Journals",
                        "type" : "Format"
                      },
                      "count" : ${worksFormatJournals.length},
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "i",
                        "label" : "Audio",
                        "type" : "Format"
                      },
                      "count" : ${worksFormatAudio.length},
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "k",
                        "label" : "Pictures",
                        "type" : "Format"
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
                        "label" : "Electronic books",
                        "concepts": [],
                        "type" : "Genre"
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
                        "label": "1098",
                        "type": "Period"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "label": "1904",
                        "type": "Period"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "label": "2020",
                        "type": "Period"
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
                        "label" : "English",
                        "type" : "Language"
                      },
                      "count" : 5,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "swe",
                        "label" : "Swedish",
                        "type" : "Language"
                      },
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "tur",
                        "label" : "Turkish",
                        "type" : "Language"
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
                        "concepts" : [],
                        "label" : "realAnalysis",
                        "type" : "Subject"
                      },
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "concepts" : [],
                        "label" : "paleoNeuroBiology",
                        "type" : "Subject"
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
                        "label" : "47",
                        "type" : "Agent"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "label" : "MI5",
                        "type" : "Organisation"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "label" : "007",
                        "type" : "Agent"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "label" : "GCHQ",
                        "type" : "Organisation"
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
                        "label" : "Attribution 4.0 International (CC BY 4.0)",
                        "type" : "License",
                        "url" : "http://creativecommons.org/licenses/by/4.0/"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "cc-by-nc",
                        "label" : "Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)",
                        "type" : "License",
                        "url" : "https://creativecommons.org/licenses/by-nc/4.0/"
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
        indexTestDocuments(worksIndex, worksEverything: _*)
        val displayWorks = getMinimalDisplayWorks(worksEverything)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=availabilities"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = worksEverything.size)},
              "aggregations": {
                "availabilities": {
                  "buckets": [
                    {
                      "count": 2,
                      "data": {
                        "label": "Closed stores",
                        "id": "closed-stores",
                        "type" : "Availability"
                      },
                      "type": "AggregationBucket"
                    },
                    {
                      "count": 1,
                      "data": {
                        "label": "Open shelves",
                        "id": "open-shelves",
                        "type" : "Availability"
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
