package weco.api.search.services

import scala.io.Source

trait ImagesTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("ImagesQuery.json").mkString

}

object ImagesTemplateSearchBuilder extends ImagesTemplateSearchBuilder
