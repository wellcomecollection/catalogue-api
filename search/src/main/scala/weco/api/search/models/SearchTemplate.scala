package weco.api.search.models

import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.json.JacksonBuilder
import com.sksamuel.elastic4s.requests.searches.queries.Query
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.Encoder
import weco.http.json.DisplayJsonUtil._

case class SearchTemplate(id: String, pipeline: String,index: String, query: String)

object SearchTemplate {
  def apply(id: String, pipeline: String, index: String, query: Query): SearchTemplate =
    SearchTemplate(
      id,
      pipeline,
      index,
      JacksonBuilder.writeAsString(QueryBuilderFn(query).value)
    )

  implicit val encoder: Encoder[SearchTemplate] =
    deriveConfiguredEncoder
}

// This is to return the search templates in the format of
// { "templates": [...] }
case class SearchTemplateResponse(templates: List[SearchTemplate])

object SearchTemplateResponse {
  implicit val encoder: Encoder[SearchTemplateResponse] =
    deriveConfiguredEncoder
}
