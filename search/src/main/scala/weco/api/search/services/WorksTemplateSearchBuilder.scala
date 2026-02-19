package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksQuery.json").mkString

  val semanticTemplateSparse: String =
    Source.fromResource("WorksSemanticQuerySparse.mustache").mkString
  val semanticTemplateDense: String =
    Source.fromResource("WorksSemanticQueryDense.mustache").mkString

  // Choose the right semantic query template based on whether the config specifies sparse or dense vectors
  lazy protected val semanticQueryTemplate: String =
    s"""
       |  {{#vectorType}}
       |  {{#Sparse}}
       |  $semanticTemplateSparse
       |  {{/Sparse}}
       |  {{#Dense}}
       |  $semanticTemplateDense
       |  {{/Dense}}
       |  {{/vectorType}}
       |""".stripMargin

  lazy protected val semanticQuery: String =
    s"""
       |{
       |  "bool": {
       |     {{#query}}
       |     "must": $semanticQueryTemplate,
       |     {{/query}}
       |     "filter": {{#toJson}}preFilter{{/toJson}}
       |  }
       | }""".stripMargin

  lazy protected val hybridQuery: String =
    s"""
       |  {
       |    "bool": {
       |      "should": [
       |        $lexicalQuery,
       |        $semanticQuery
       |      ],
       |      "minimum_should_match": 1
       |    }
       |  }
       |""".stripMargin

  lazy protected val hybridRetrieverQuery: String =
    s"""
       |  {
       |    "rrf": {
       |      "retrievers": [
       |        {
       |          "standard": {
       |            "query": $lexicalQuery
       |          }
       |        },
       |        {
       |          "standard": {
       |            "query": $semanticQuery
       |          }
       |        }
       |      ],
       |      "rank_window_size": {{rankWindowSize}},
       |      "rank_constant": {{rankConstant}}
       |    }
       |  }
       |""".stripMargin

  // If semantic config is specified, run a hybrid query consisting of a lexical and a semantic component:
  //   * When sorting by score (default), use a reciprocal rank fusion (RRF) retriever to combine the two components.
  //   * When sorting by date, do not use RRF (sorting and RRF retrieval are incompatible).
  override protected lazy val source: String =
    normaliseSource(
      s"""
       |{
       |  {{#semanticConfig}}
       |    {{#sortByDate}}
       |    "query": $hybridQuery,
       |    "sort": $sort,
       |    {{/sortByDate}}
       |    {{^sortByDate}}
       |    "retriever": $hybridRetrieverQuery,
       |    {{/sortByDate}}
       |  {{/semanticConfig}}
       |  {{^semanticConfig}}
       |  "query": $lexicalQuery,
       |  "sort": $sort,
       |  {{/semanticConfig}}
       |  $commonQueryFields
       |}
       |""".stripMargin
    )
}

object WorksTemplateSearchBuilder extends WorksTemplateSearchBuilder
