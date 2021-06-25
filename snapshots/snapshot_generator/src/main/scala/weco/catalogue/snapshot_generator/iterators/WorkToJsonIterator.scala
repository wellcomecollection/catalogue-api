package weco.catalogue.snapshot_generator.iterators

import weco.catalogue.display_model.models.Implicits._
import weco.catalogue.display_model.models.DisplayWork
import weco.catalogue.display_model.models.{DisplayWork, WorksIncludes}
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.http.json.DisplayJsonUtil

object WorkToJsonIterator {
  def apply(works: Iterator[Work.Visible[Indexed]]): Iterator[String] =
    works
      .map { w =>
        DisplayWork.apply(w, WorksIncludes.all)
      }
      .map { dw =>
        DisplayJsonUtil.toJson(dw)
      }
}
