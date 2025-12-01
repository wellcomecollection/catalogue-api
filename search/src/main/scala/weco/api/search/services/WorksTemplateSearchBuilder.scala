package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksQuery.json").mkString

}

object WorksTemplateSearchBuilder extends WorksTemplateSearchBuilder
