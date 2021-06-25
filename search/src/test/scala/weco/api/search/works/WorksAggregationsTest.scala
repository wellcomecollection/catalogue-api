package weco.api.search.works

import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.generators.{
  ItemsGenerators,
  ProductionEventGenerators
}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.languages.Language
import weco.catalogue.internal_model.locations.{AccessStatus, License}
import weco.catalogue.internal_model.work._
import weco.catalogue.internal_model.work.Format._

class WorksAggregationsTest
    extends ApiWorksTestBase
    with ItemsGenerators
    with ProductionEventGenerators {

  it("supports fetching the format aggregation") {
    withWorksApi {
      case (worksIndex, routes) =>
        val formats = List(
          Books,
          Books,
          Books,
          Pictures,
          Pictures,
          Journals
        )

        val works = formats.map { indexedWork().format(_) }

        insertIntoElasticsearch(worksIndex, works: _*)

        assertJsonResponse(routes, s"$rootPath/works?aggregations=workType") {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
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
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "k",
                        "label" : "Pictures",
                        "type" : "Format"
                      },
                      "count" : 2,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : {
                        "id" : "d",
                        "label" : "Journals",
                        "type" : "Format"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${works
            .sortBy { _.state.canonicalId }
            .map(workResponse)
            .mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("supports fetching the genre.label aggregation") {
    withWorksApi {
      case (worksIndex, routes) =>
        val concept0 = Concept("conceptLabel")
        val concept1 = Place("placeLabel")
        val concept2 = Period(
          id = IdState.Identified(
            canonicalId = createCanonicalId,
            sourceIdentifier = createSourceIdentifierWith(
              ontologyType = "Period"
            )
          ),
          label = "periodLabel",
          range = None
        )

        val genre = Genre(
          label = "Electronic books.",
          concepts = List(concept0, concept1, concept2)
        )

        val work = indexedWork().genres(List(genre))

        insertIntoElasticsearch(worksIndex, work)

        assertJsonResponse(routes, s"$rootPath/works?aggregations=genres.label") {
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
                        "label" : "conceptLabel",
                        "concepts": [],
                        "type" : "Genre"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    },
                           {
                      "data" : {
                        "label" : "periodLabel",
                        "concepts": [],
                        "type" : "Genre"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    },
                           {
                      "data" : {
                        "label" : "placeLabel",
                        "concepts": [],
                        "type" : "Genre"
                      },
                      "count" : 1,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${workResponse(work)}]
            }
          """
        }
    }
  }

  it("supports aggregating on dates by from year") {
    withWorksApi {
      case (worksIndex, routes) =>
        val dates = List("1st May 1970", "1970", "1976", "1970-1979")

        val works = dates
          .map { dateLabel =>
            indexedWork()
              .production(
                List(createProductionEventWith(dateLabel = Some(dateLabel)))
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

  it("supports aggregating on languages") {
    val english = Language(label = "English", id = "eng")
    val swedish = Language(label = "Swedish", id = "swe")
    val turkish = Language(label = "Turkish", id = "tur")

    val works = Seq(
      indexedWork().languages(List(english)),
      indexedWork().languages(List(english, swedish)),
      indexedWork().languages(List(english, swedish, turkish))
    )

    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, works: _*)
        assertJsonResponse(routes, s"$rootPath/works?aggregations=languages") {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
              "aggregations": {
                "type" : "Aggregations",
                "languages": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : ${language(english)},
                      "count" : 3,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : ${language(swedish)},
                      "count" : 2,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : ${language(turkish)},
                      "count" : 1,
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

  it("supports aggregating on items.locations.license") {
    def createLicensedWork(
      licenses: Seq[License]
    ): Work.Visible[WorkState.Indexed] = {
      val items =
        licenses.map { license =>
          createDigitalItemWith(license = Some(license))
        }.toList

      indexedWork().items(items)
    }

    val licenseLists = List(
      List(License.CCBY),
      List(License.CCBY),
      List(License.CCBYNC),
      List(License.CCBY, License.CCBYNC),
      List.empty
    )

    val works = licenseLists.map { createLicensedWork(_) }

    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, works: _*)
        assertJsonResponse(
          routes,
          s"$rootPath/works?aggregations=items.locations.license"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
              "aggregations": {
                "type" : "Aggregations",
                "items.locations.license": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 3,
                      "data" : ${license(License.CCBY)},
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : ${license(License.CCBYNC)},
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

  it("supports aggregating on availabilities") {
    val items = List(
      List(createIdentifiedPhysicalItem),
      List(createIdentifiedPhysicalItem),
      List(createDigitalItemWith(accessStatus = AccessStatus.Open)),
      List(createDigitalItemWith(accessStatus = AccessStatus.Open)),
      List(createDigitalItemWith(accessStatus = AccessStatus.OpenWithAdvisory)),
      List(
        createIdentifiedPhysicalItem,
        createDigitalItemWith(accessStatus = AccessStatus.Open)
      )
    )
    val works = items.map(indexedWork().items(_))

    withWorksApi {
      case (worksIndex, routes) =>
        insertIntoElasticsearch(worksIndex, works: _*)
        assertJsonResponse(
          routes = routes,
          path = s"$rootPath/works?aggregations=availabilities"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = works.size)},
              "aggregations": {
                "availabilities": {
                  "buckets": [
                    {
                      "count": 4,
                      "data": {
                        "label": "Online",
                        "id": "online",
                        "type" : "Availability"
                      },
                      "type": "AggregationBucket"
                    },
                    {
                      "count": 3,
                      "data": {
                        "label": "In the library",
                        "id": "in-library",
                        "type" : "Availability"
                      },
                      "type": "AggregationBucket"
                    }
                  ],
                  "type": "Aggregation"
                },
                "type": "Aggregations"
              },
              "results": [${works
                            .sortBy { _.state.canonicalId }
                            .map(workResponse)
                            .mkString(",")}]
            }
          """.stripMargin
        }
    }
  }
}