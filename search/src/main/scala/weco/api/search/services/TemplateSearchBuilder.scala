package weco.api.search.services

import com.sksamuel.elastic4s.Indexes
import io.circe.Json
import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest

trait TemplateSearchBuilder {
  protected val queryTemplate: String

  lazy protected val source: String =
    s"""
       |{ "query": $queryTemplate,
       |  "from": "{{from}}",
       |  "size": "{{size}}",
       |  "_source": {
       |    "includes": [
       |      "display",
       |      "type"
       |    ]
       |  },
       |  {{#aggs}}
       |  "aggs": {{#toJson}}aggs{{/toJson}},
       |  {{/aggs}}
       |  {{#post_filters}}
       |  "post_filter": {{#toJson}}post_filters{{/toJson}},
       |  {{/post_filters}}
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

  def searchRequest(indexes: Indexes, params: Json): TemplateSearchRequest =
    TemplateSearchRequest(indexes, source, params)
}
