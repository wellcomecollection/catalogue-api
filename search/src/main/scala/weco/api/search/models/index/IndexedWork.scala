package weco.api.search.models.index

import io.circe.Json

sealed trait IndexedWork

case class Identified(canonicalId: String)

object IndexedWork {
  case class Visible(display: Json) extends IndexedWork

  case class Redirected(redirectTarget: Identified) extends IndexedWork

  case class Invisible() extends IndexedWork
  case class Deleted() extends IndexedWork
}
