package weco.api.search.models.index

import io.circe.Json
import io.circe.syntax._
import weco.catalogue.display_model.work.DisplayWork
import weco.catalogue.display_model.Implicits._
import weco.catalogue.internal_model.work.{Work, WorkState}

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

  def apply(work: Work[WorkState.Indexed]): IndexedWork =
    work match {
      case w: Work.Visible[WorkState.Indexed] =>
        IndexedWork.Visible(DisplayWork(w).asJson)

      case w: Work.Redirected[WorkState.Indexed] =>
        IndexedWork.Redirected(
          redirectTarget =
            Identified(canonicalId = w.redirectTarget.canonicalId.underlying)
        )

      case _: Work.Invisible[WorkState.Indexed] =>
        IndexedWork.Invisible()

      case _: Work.Deleted[WorkState.Indexed] =>
        IndexedWork.Deleted()
    }
}
