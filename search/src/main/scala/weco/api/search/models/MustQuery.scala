package weco.api.search.models

sealed trait WorkMustQuery

sealed trait ImageMustQuery

case class ColorMustQuery(colors: Seq[HsvColor]) extends ImageMustQuery
