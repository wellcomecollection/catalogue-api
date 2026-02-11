package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksQuery.json").mkString

  val semanticTemplateSparse: String = Source.fromResource("WorksSemanticQuerySparse.mustache").mkString
  val semanticTemplateDense: String = Source.fromResource("WorksSemanticQueryDense.mustache").mkString
  lazy protected val semanticQueryTemplate: String =
    s"""
       |  {{#semanticIsSparse}}
       |  $semanticTemplateSparse
       |  {{/semanticIsSparse}}
       |  {{^semanticIsSparse}}
       |  $semanticTemplateDense
       |  {{/semanticIsSparse}}
       |""".stripMargin

  lazy protected val hybridQuery: String =
    s"""
       |  {
       |    "bool": {
       |      "should": [
       |        $lexicalQuery,
       |        {
       |          "bool": {
       |            "must": $semanticQueryTemplate,
       |            "filter": {{#toJson}}preFilter{{/toJson}}
       |          }
       |        }
       |      ],
       |      "minimum_should_match": 1
       |    }
       |  }
       |""".stripMargin

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
       |            "query": {
       |              "bool": {
       |                "must": $semanticQueryTemplate,
       |                "filter": {{#toJson}}preFilter{{/toJson}}
       |              }
       |            }
       |          }
       |        }
       |      ],
       |      "rank_window_size": 10000,
       |      "rank_constant": 20
       |    }
       |  }
       |""".stripMargin

  override protected lazy val source: String =
    normaliseSource(
      s"""
       |{
       |  {{#includeSemantic}}
       |    {{#sortByDate}}
       |    "query": $hybridQuery,
       |    "sort": $sort,
       |    {{/sortByDate}}
       |    {{^sortByDate}}
       |    "retriever": $retrieverQuery,
       |    {{/sortByDate}}
       |  {{/includeSemantic}}
       |  {{^includeSemantic}}
       |  "query": $lexicalQuery,
       |  "sort": $sort,
       |  {{/includeSemantic}}
       |  $commonQueryFields
       |}
       |""".stripMargin
     )

}

object WorksTemplateSearchBuilder extends WorksTemplateSearchBuilder
