package weco.api.requests.models

import io.circe.Json
import io.circe.parser._
import weco.api.stacks.models.CatalogueWork
import weco.catalogue.display_model.Implicits._
import weco.catalogue.display_model.work.DisplayItem
import weco.http.json.DisplayJsonUtil

case class RequestedItemWithWork(
  workId: String,
  workTitle: Option[String],
  item: Json
)

case object RequestedItemWithWork {
  def apply(item: DisplayItem, work: CatalogueWork): RequestedItemWithWork =
    RequestedItemWithWork(work.id, work.title, item)

  def apply(
    workId: String,
    workTitle: Option[String],
    item: DisplayItem
  ): RequestedItemWithWork =
    RequestedItemWithWork(
      workId,
      workTitle,
      item = parse(DisplayJsonUtil.toJson(item)).right.get
    )
}
