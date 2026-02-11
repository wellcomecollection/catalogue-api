package weco.api.search.services

import scala.io.Source

trait ImagesTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("ImagesQuery.json").mkString

  // KNN based searches rank by similarity, meaning that the complexity
  // of a full query is not required, and could be misleading to read.
  // A query may have boosts for finding things like exact matches
  // or values in certain fields, and, if copied into the knn filter
  // would be executed in filter context and be discarded.
  // When running a KNN search, the query term becomes a simple filter
  // on whether the document contains the term in any searchable field.
  // If there are fields that should be ignored, then this default will
  // be inappropriate, and can be overridden.
  protected val knnFilter: String = """
    |{
    |  "multi_match": {
    |    "query": "{{query}}",
    |    "operator": "and",
    |    "type": "best_fields",
    |    "tie_breaker": 0.4,
    |    "fields": "query.*"
    |  }
    |}
    |""".stripMargin

  lazy private val knnQuery: String =
    s"""
       |  {
       |    {{#query}}
       |    "filter": $knnFilter,
       |    {{/query}}
       |    {{#similarityThreshold}}
       |    "similarity": {{similarityThreshold}},
       |    {{/similarityThreshold}}
       |    "field": "{{field}}",
       |    "k": {{k}},
       |    "num_candidates": {{numCandidates}},
       |    "query_vector": {{#toJson}}queryVector{{/toJson}}
       |  }
       |""".stripMargin

  override protected lazy val source: String =
    normaliseSource(
      s"""
       |{
       |  {{#knn}}
       |  "knn": $knnQuery,
       |  {{/knn}}
       |  {{^knn}}
       |  "query": $lexicalQuery,
       |  {{/knn}}
       |  "sort": $sort,
       |  $commonQueryFields
       |}
       |""".stripMargin
    )

}

object ImagesTemplateSearchBuilder extends ImagesTemplateSearchBuilder
