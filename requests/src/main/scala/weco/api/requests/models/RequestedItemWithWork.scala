package weco.api.requests.models

import io.circe.Json
import io.circe.parser._
import weco.catalogue.display_model.Implicits._
import weco.catalogue.display_model.work.DisplayItem
import weco.http.json.DisplayJsonUtil

case class RequestedItemWithWork(
  workId: String,
  workTitle: Option[String],
  item: Json
)

case object RequestedItemWithWork {
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
