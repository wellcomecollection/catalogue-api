package weco.api.search.elasticsearch.templateSearch

import com.sksamuel.elastic4s.Indexes
import io.circe.Json

case class TemplateSearchRequest(
  indexes: Indexes,
  source: String,
  params: Json
)
