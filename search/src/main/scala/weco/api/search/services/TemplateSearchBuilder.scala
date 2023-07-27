package weco.api.search.services

import io.circe.Json
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest

trait TemplateSearchBuilder extends Encoders {
  val queryTemplate: String

  lazy protected val source: String =
    s"""
       |{ {{#query}}
       |  "query": $queryTemplate,
       |  {{/query}}
       |
       |  "from": "{{from}}",
       |  "size": "{{size}}",
       |  "_source": {
       |    "includes": {{#toJson}}includes{{/toJson}}
       |  },
       |
       |  {{#aggs}}
       |  "aggs": {{#toJson}}aggs{{/toJson}},
       |  {{/aggs}}
       |
       |  {{#postFilter}}
       |  "post_filter": {{#toJson}}postFilter{{/toJson}},
       |  {{/postFilter}}
       |
       |  "sort": [
       |    {{#sortByDate}}
       |    {
       |      "query.production.dates.range.from": {
       |        "order": "{{sortByDate}}"
       |      }
       |    },
       |    {{/sortByDate}}
       |    {{#sortByScore}}
       |    {
       |      "_score": {
       |        "order": "desc"
       |      }
       |    },
       |    {{/sortByScore}}
       |    {
       |      "query.id": {
       |        "order": "asc"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin

  def searchRequest(indexes: Seq[String], params: Json): TemplateSearchRequest =
    TemplateSearchRequest(indexes, source, params)

}
