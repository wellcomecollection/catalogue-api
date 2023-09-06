package weco.api.search.services

import scala.io.Source

trait ImagesTemplateSearchBuilder extends TemplateSearchBuilder {

  protected val queryTemplate: String =
    Source.fromResource("ImagesMultiMatcherQueryTemplate.json").mkString

  protected val dateField: String = "query.source.production.dates.range.from"

}

object ImagesTemplateSearchBuilder extends ImagesTemplateSearchBuilder
