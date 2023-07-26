package weco.api.search.services

import io.circe.Json
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest

trait TemplateSearchBuilder {
  protected val queryTemplate: String

  lazy protected val source: String =
    s"""
       |{ {{#query}}
       |  "query": $queryTemplate,
       |  {{/query}}
       |  "from": "{{from}}",
       |  "size": "{{size}}",
       |  "_source": {
       |    "includes": [
       |      "display",
       |      "type"
       |    ]
       |  },
       |
       |  {{#aggs}}
       |  "aggs": {{#toJson}}aggs{{/toJson}},
       |  {{/aggs}}
       |
       |  {{#post_filter}}
       |  "post_filter": {{#toJson}}post_filter{{/toJson}},
       |  {{/post_filter}}
       |
       |  "sort": [
       |    {{#sort_by_date}}
       |    {
       |      "query.production.dates.range.from": {
       |        "order": "{{sort_by_date}}"
       |      }
       |    },
       |    {{/sort_by_date}}
       |    {{#sort_by_score}}
       |    {
       |      "_score": {
       |        "order": "desc"
       |      }
       |    },
       |    {{/sort_by_score}}
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
