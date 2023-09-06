package weco.api.search.services

import scala.io.Source

trait ImagesTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("ImagesMultiMatcherQueryTemplate.json").mkString

  val dateField: String = "query.source.production.dates.range.from"

}

object ImagesTemplateSearchBuilder extends ImagesTemplateSearchBuilder