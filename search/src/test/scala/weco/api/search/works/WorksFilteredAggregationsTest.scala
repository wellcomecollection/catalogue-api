package weco.api.search.works

import io.circe.syntax.EncoderOps
import org.scalatest.funspec.AnyFunSpec
import weco.api.search.generators.AggregationDocumentGenerators
import weco.api.search.models.request.WorksIncludes

import java.net.URLEncoder

class WorksFilteredAggregationsTest
    extends AnyFunSpec
    with ApiWorksTestBase
    with AggregationDocumentGenerators {
  val aggregatedWorks =
    (0 to 9).map(i => s"works.examples.filtered-aggregations-tests.$i")

  val worksBooks =
    Seq(0, 4, 5, 7)
      .map(i => s"works.examples.filtered-aggregations-tests.$i")
      .map { getVisibleWork }
      .map(_.display.withIncludes(WorksIncludes.none))
      .sortBy(w => getKey(w, "id").get.asString)

  val worksBooksAboutRats =
    Seq(0)
      .map(i => s"works.examples.filtered-aggregations-tests.$i")
      .map { getVisibleWork }
      .map(_.display.withIncludes(WorksIncludes.none))
      .sortBy(w => getKey(w, "id").get.asString)

  describe(
    "filters aggregation buckets with any filters that are not paired to the aggregation"
  ) {
    it("when those filters do not have a paired aggregation present") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, aggregatedWorks: _*)

          assertJsonResponse(
            routes,
            // We expect to see the language buckets for only the works with workType=a
            path = s"$rootPath/works?workType=a&aggregations=languages"
          ) {
            Status.OK -> s"""
            {
              ${resultList(totalResults = worksBooks.length)},
              "aggregations": {
                "type" : "Aggregations",
                "languages": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 3,
                      "data" : {
                        "id" : "bak",
                        "label" : "Bashkir"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "mar",
                        "label" : "Marathi"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${worksBooks.mkString(",")}]
            }
          """.stripMargin
          }
      }
    }

    it("when those filters do have a paired aggregation present") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, aggregatedWorks: _*)

          assertJsonResponse(
            routes,
            // We expect to see the language buckets for only the works with workType=a
            // We expect to see the workType buckets for all of the works
            path = s"$rootPath/works?workType=a&aggregations=languages,workType"
          ) {
            Status.OK -> s"""
            {
              ${resultList(totalResults = worksBooks.length)},
              "aggregations": {
                "type" : "Aggregations",
                "languages": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 3,
                      "data" : {
                        "id" : "bak",
                        "label" : "Bashkir"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "mar",
                        "label" : "Marathi"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                },
                "workType" : {
                  "type": "Aggregation",
                  "buckets" : [
                    {
                      "count" : 4,
                      "data" : {
                        "id" : "a",
                        "label" : "Books"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 3,
                      "data" : {
                        "id" : "d",
                        "label" : "Journals"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "i",
                        "label" : "Audio"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "k",
                        "label" : "Pictures"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${worksBooks.mkString(",")}]
            }
          """.stripMargin
          }
      }
    }

    it("but still returns empty buckets if their paired filter is present") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, aggregatedWorks: _*)

          assertJsonResponse(
            routes,
            // We expect to see the workType buckets for worktype i/Audio, because that
            // has the language che/Chechen, and for a/Books, because a filter for it is
            // present
            path =
              s"$rootPath/works?workType=a&languages=che&aggregations=workType"
          ) {
            Status.OK ->
              s"""
            {
              ${resultList(totalResults = 0, totalPages = 0)},
              "aggregations": {
                "type" : "Aggregations",
                "workType" : {
                  "type": "Aggregation",
                  "buckets" : [
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "i",
                        "label" : "Audio"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 0,
                      "data" : {
                        "id" : "a",
                        "label" : "Books"
                      },
                      "type" : "AggregationBucket"
                    } 
                  ]
                }
              },
              "results": []
            }
          """.stripMargin
          }
      }
    }
  }
  it("safely handles terms with special characters") {
    withWorksApi {
      case (worksIndex, routes) =>
        val doc = createWorkDocument(
          "xkcd0327",
          "Exploits Of a Mom",
          Map("contributors.agent" -> Seq(createAggregatableField("Robert .?+*|{}<>&@[]()\" Tables"))),
          Map("contributors.agent.label" -> Seq("Robert .?+*|{}<>&@[]()\" Tables").asJson)
        )

        indexLoadedTestDocuments(worksIndex, Seq(doc))

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/works?contributors.agent.label=${URLEncoder.encode("Robert .?+*|{}<>&@[]()\" Tables", "UTF-8")}&aggregations=contributors.agent.label"
        ) {
          Status.OK ->
            s"""
          {
            ${resultList(totalResults = 1)},
            "results":[{"id":"xkcd0327","title":"Exploits Of a Mom"}],
            "aggregations": {
              "contributors.agent.label": {
                "buckets": [{
                  "data": {
                    "id" : "Robert .?+*|{}<>&@[]()\\" Tables",
                    "label": "Robert .?+*|{}<>&@[]()\\" Tables"
                    },
                    "count":1,
                    "type":"AggregationBucket"
                  }],
                  "type":"Aggregation"
                },
                "type":"Aggregations"
              }
           }
        """.stripMargin
        }
    }
  }

  it("applies the search query to aggregations paired with an applied filter") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, aggregatedWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?query=rats&workType=a&aggregations=workType"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = worksBooksAboutRats.length)},
              "aggregations": {
                "type" : "Aggregations",
                "workType": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "i",
                        "label" : "Audio"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "a",
                        "label" : "Books"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "d",
                        "label" : "Journals"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${worksBooksAboutRats.mkString(",")}]
            }
          """.stripMargin
        }
    }
  }

  it("filters results but not aggregations paired with an applied filter") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, aggregatedWorks: _*)

        assertJsonResponse(
          routes,
          s"$rootPath/works?workType=a&aggregations=workType"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = worksBooks.length)},
              "aggregations": {
                "type" : "Aggregations",
                "workType": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "count" : 4,
                      "data" : {
                        "id" : "a",
                        "label" : "Books"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 3,
                      "data" : {
                        "id" : "d",
                        "label" : "Journals"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 2,
                      "data" : {
                        "id" : "i",
                        "label" : "Audio"
                      },
                      "type" : "AggregationBucket"
                    },
                    {
                      "count" : 1,
                      "data" : {
                        "id" : "k",
                        "label" : "Pictures"
                      },
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [${worksBooks.mkString(",")}]
            }
          """.stripMargin
        }
    }
  }
}
