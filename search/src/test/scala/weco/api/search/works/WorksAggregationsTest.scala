package weco.api.search.works

class WorksAggregationsTest extends ApiWorksTestBase {
  it("aggregates by format") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksFormat: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?aggregations=workType&pageSize=1"
        ) {
          Status.OK -> s"""
            {
              ${resultList(
            totalResults = worksFormat.size,
            pageSize = 1,
            totalPages = worksFormat.size
          )},
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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "3uu0bujc",
                  "title" : "A work with format Journals",
                  "type" : "Work",
                  "workType" : {
                    "id" : "d",
                    "label" : "Journals",
                    "type" : "Format"
                  }
                }
              ],
              "nextPage" : "$publicRootUri/works?aggregations=workType&pageSize=1&page=2"
            }
          """
        }
    }
  }

  it("aggregates by genre label") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.genres")

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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "lhrstrzi",
                  "title" : "A work with different concepts in the genre",
                  "type" : "Work"
                }
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
                        "label": "1097",
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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "ko9eldlc",
                  "title" : "Production event in 1904",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "minusjlh",
                  "title" : "Production event in 2020",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "mxme7pvj",
                  "title" : "Production event in 1098",
                  "type" : "Work"
                }
              ]
            }
          """
        }
    }
  }

  it("aggregates by language") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "works.languages.0.eng",
          "works.languages.1.eng",
          "works.languages.2.eng",
          "works.languages.3.eng+swe",
          "works.languages.4.eng+swe+tur",
          "works.languages.5.swe",
          "works.languages.6.tur"
        )

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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "5a37bi10",
                  "title" : "A work with languages English",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "j78ps149",
                  "title" : "A work with languages English, Swedish",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "m6ne7xwr",
                  "title" : "A work with languages English",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "ry03jkbp",
                  "title" : "A work with languages Swedish",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "s3mu3txt",
                  "title" : "A work with languages English",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "xmkstgwq",
                  "title" : "A work with languages Turkish",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "xqjkrlx5",
                  "title" : "A work with languages English, Swedish, Turkish",
                  "type" : "Work"
                }
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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "5ymcwk8h",
                  "title" : "title-KD9r1zHmiH",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "6jxrk5e3",
                  "title" : "title-by26w1evrA",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "p9ugies0",
                  "title" : "title-LgxSR5EK6U",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "u7atgijp",
                  "title" : "title-v2pMEZn8L2",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "wrkxloww",
                  "title" : "title-bkm5UMJEwT",
                  "type" : "Work"
                }
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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "hui5cjlp",
                  "title" : "title-MGlgVkz3Vf",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "hutjlml0",
                  "title" : "title-H1Bj9evx0c",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "pkf3m7xe",
                  "title" : "title-xv45oXcssa",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "xilf8imb",
                  "title" : "title-PHmut9nJ8z",
                  "type" : "Work"
                }
              ]
            }
          """
        }
    }
  }

  it("aggregates by item license") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          (0 to 4).map(i => s"works.items-with-licenses.$i"): _*
        )

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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "aaipgbth",
                  "title" : "title-xzEZGWEGMy",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "fx49bqty",
                  "title" : "title-WIjswjHR3o",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "pak011dv",
                  "title" : "title-n9x0HfU454",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "tbgvrpdz",
                  "title" : "title-zGimVHoe2r",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [],
                  "id" : "wjvsf3c3",
                  "title" : "title-67fbITZO5o",
                  "type" : "Work"
                }
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
                {
                  "alternativeTitles" : [],
                  "availabilities" : [
                    {
                      "id" : "closed-stores",
                      "label" : "Closed stores",
                      "type" : "Availability"
                    }
                  ],
                  "id" : "oo9fg6ic",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [
                    {
                      "id" : "open-shelves",
                      "label" : "Open shelves",
                      "type" : "Availability"
                    },
                    {
                      "id" : "closed-stores",
                      "label" : "Closed stores",
                      "type" : "Availability"
                    }
                  ],
                  "id" : "ou9z1esm",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [],
                  "availabilities" : [
                    {
                      "id" : "closed-stores",
                      "label" : "Closed stores",
                      "type" : "Availability"
                    }
                  ],
                  "id" : "wchkoofm",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
              ]
            }
          """.stripMargin
        }
    }
  }
}
