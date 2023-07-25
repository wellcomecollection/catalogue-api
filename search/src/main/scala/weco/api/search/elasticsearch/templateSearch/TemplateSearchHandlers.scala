package weco.api.search.elasticsearch.templateSearch

import com.sksamuel.elastic4s.{
  ElasticRequest,
  ElasticUrlEncoder,
  Handler,
  HttpEntity,
  Indexes
}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Json
import io.circe.syntax.EncoderOps

trait TemplateSearchHandlers {
  implicit object TemplateSearchHandler
      extends Handler[TemplateSearchRequest, SearchResponse] {

    private def endpoint(indexes: Indexes): String =
      if (indexes.values.isEmpty)
        "/_all/_search/template"
      else
        "/" + indexes.values
          .map(ElasticUrlEncoder.encodeUrlFragment)
          .mkString(",") + "/_search/template"

    override def build(request: TemplateSearchRequest): ElasticRequest = {
      val body = Json
        .obj(
          "source" -> request.source.asJson,
          "params" -> request.params
        )
        .noSpaces

      ElasticRequest(
        method = "POST",
        endpoint = endpoint(request.indexes),
        body = HttpEntity(body, "application/json")
      )
    }
  }

}
