package weco.api.search.elasticsearch.templateSearch

import io.circe.Json

case class TemplateSearchRequest(
  indexes: Seq[String],
  source: String,
  params: Json
)
