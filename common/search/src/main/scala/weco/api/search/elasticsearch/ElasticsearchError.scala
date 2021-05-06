package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticError

sealed trait ElasticsearchError

case object ElasticsearchError {
  def apply(e: ElasticError): ElasticsearchError =
    e.`type` match {
      case "index_not_found_exception" => IndexNotFoundError(e)

      case "search_phase_execution_exception" => SearchPhaseExecutionError(e)

      case _ => UnknownError(e)
    }
}

case class DocumentNotFoundError[Id](id: Id) extends ElasticsearchError

case class IndexNotFoundError(e: ElasticError) extends ElasticsearchError {
  def index: String = e.index.get
}

case class SearchPhaseExecutionError(e: ElasticError) extends ElasticsearchError {
  def reason: String = e.rootCause.mkString("; ")
}

case class UnknownError(e: ElasticError) extends ElasticsearchError
