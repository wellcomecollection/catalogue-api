package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksQuery.json").mkString

  val semanticTemplateSparse: String =
    Source.fromResource("WorksSemanticQuerySparse.mustache").mkString
  val semanticTemplateDense: String =
    Source.fromResource("WorksSemanticQueryDense.mustache").mkString
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

  lazy protected val retrieverQuery: String =
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
       |    "retriever": $retrieverQuery,
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
