package weco.api.search.services

import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

trait TemplateSearchBuilder extends Encoders {
  // Template for the "query" part of the request.
  // This is expected to be a mustache template in which
  // the query term to be used in the search is represented by a variable
  // called "query".
  // This preserves the existing behaviour of /search-templates.json
  val queryTemplate: String

  // Importantly, this is *not* JSON, due to the `{{#` sequences, so must be created and sent as a string.
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

  def searchRequest(
    indexes: Seq[String],
    params: SearchTemplateParams
  ): TemplateSearchRequest =
    TemplateSearchRequest(indexes, source, params.asJson)

}
