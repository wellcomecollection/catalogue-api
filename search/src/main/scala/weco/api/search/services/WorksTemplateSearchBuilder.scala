package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksQuery.json").mkString

  protected val dateField: String =
    "filterableValues.production.dates.range.from"

}

object WorksTemplateSearchBuilder extends WorksTemplateSearchBuilder
