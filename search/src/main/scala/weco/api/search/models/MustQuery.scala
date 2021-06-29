package weco.api.search.models

sealed trait WorkMustQuery

sealed trait ImageMustQuery

case class ColorMustQuery(hexColors: Seq[String]) extends ImageMustQuery
