package weco.api.search.elasticsearch.templateSearch

import com.sksamuel.elastic4s.{
  ElasticRequest,
  ElasticUrlEncoder,
  Handler,
  HttpEntity
}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import io.circe.Json
import io.circe.syntax.EncoderOps

/**
  * Handler used by E4S ElasticClient.execute to turn a
  * request (the E4S-style description of what we want the request to contain)
  * into an ElasticRequest (the method/url/body combination that will do that)
  *
  * The template search implementation built in to E4S does not currently support
  * sending the template in the source attribute.  It only works with templates that
  * have been stored on the cluster.
  *
  * We do not want to permit the API to make cluster changes, and although (in the future)
  * we may wish to configure the cluster at deploy time to contain the template,
  * it is currently a step too far in terms of maintenance and ownership of the queries.
  */
trait TemplateSearchHandlers {
  implicit object TemplateSearchHandler
      extends Handler[TemplateSearchRequest, SearchResponse] {

    private def endpoint(indexes: Seq[String]): String =
      indexes match {
        case Nil => "/_all/_search/template"
        case _ =>
          "/" + indexes
            .map(ElasticUrlEncoder.encodeUrlFragment)
            .mkString(",") + "/_search/template"
      }

    override def build(request: TemplateSearchRequest): ElasticRequest = {
      val body = Json
        .obj(
          // The source is a string containing an Elasticsearch-flavoured Moustache template.
          // It may look like Json at a glance, and most of it is, but as a whole thing, it is not,
          // so must be sent as a String containing escaped Json content.
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
