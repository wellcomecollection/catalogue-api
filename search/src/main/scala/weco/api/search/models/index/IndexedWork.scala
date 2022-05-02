package weco.api.search.models.index

import io.circe.Json
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.WorksIncludes
import weco.catalogue.internal_model.work.{Work, WorkState}

sealed trait IndexedWork

case class Identified(canonicalId: String)

object IndexedWork extends CatalogueJsonUtil {
  case class Visible(display: Json) extends IndexedWork

  // Note: this layer of indirection is because of the current index structure,
  // which is based on internal model and Work.Redirected; at some point we
  // should flatten this out.
  case class Redirected(redirectTarget: Identified) extends IndexedWork

  case class Invisible() extends IndexedWork
  case class Deleted() extends IndexedWork

  // TODO: These are temporary methods used while we're moving the API to
  // use the "display" field in the index; once that work is done we can
  // delete this.
  object Visible {
    def apply(work: Work.Visible[WorkState.Indexed]): IndexedWork.Visible =
      IndexedWork.Visible(work.asJson(WorksIncludes.all))
  }

  def apply(work: Work[WorkState.Indexed]): IndexedWork =
    work match {
      case w: Work.Visible[WorkState.Indexed] =>
        IndexedWork.Visible(w)

      case w: Work.Redirected[WorkState.Indexed] =>
        IndexedWork.Redirected(redirectTarget = Identified(canonicalId = w.redirectTarget.canonicalId.underlying))

      case w: Work.Invisible[WorkState.Indexed] =>
        IndexedWork.Invisible()

      case w: Work.Deleted[WorkState.Indexed] =>
        IndexedWork.Deleted()
    }
}
