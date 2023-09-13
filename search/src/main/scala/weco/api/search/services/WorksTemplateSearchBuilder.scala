package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksMultiMatcherQueryTemplate.json").mkString

  protected val dateField: String = "query.production.dates.range.from"

}

object WorksTemplateSearchBuilder extends WorksTemplateSearchBuilder
