package weco.catalogue.snapshot_generator.iterators

import uk.ac.wellcome.display.models.Implicits._
import uk.ac.wellcome.display.models.{DisplayWork, WorksIncludes}
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
