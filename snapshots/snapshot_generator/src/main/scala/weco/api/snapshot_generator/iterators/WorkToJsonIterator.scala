package weco.api.snapshot_generator.iterators

import weco.catalogue.display_model.Implicits._
import weco.catalogue.display_model.work.DisplayWork
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.http.json.DisplayJsonUtil

object WorkToJsonIterator {
  def apply(works: Iterator[Work.Visible[Indexed]]): Iterator[String] =
    works
      .map { DisplayWork(_) }
      .map { DisplayJsonUtil.toJson(_) }
}
