package weco.api.search.services

import scala.io.Source

trait ImagesTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("ImagesQuery.json").mkString

  protected val dateField: String = "filterableValues.source.production.dates.range.from"

}

object ImagesTemplateSearchBuilder extends ImagesTemplateSearchBuilder
