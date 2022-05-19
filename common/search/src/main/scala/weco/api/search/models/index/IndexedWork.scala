package weco.api.search.models.index

import io.circe.Json

sealed trait IndexedWork

case class Identified(canonicalId: String)

object IndexedWork {
  case class Visible(display: Json) extends IndexedWork

  // Note: this layer of indirection is because of the current index structure,
  // which is based on internal model and Work.Redirected; at some point we
  // should flatten this out.
  case class Redirected(redirectTarget: Identified) extends IndexedWork

  case class Invisible() extends IndexedWork
  case class Deleted() extends IndexedWork
}
