package weco.api.search.json

import io.circe.Json
import io.circe.syntax._
import weco.catalogue.display_model.models.Implicits._
import weco.catalogue.display_model.models.{
  DisplayImage,
  DisplayWork,
  MultipleImagesIncludes,
  WorksIncludes
}
import weco.catalogue.internal_model.image.{Image, ImageState}
import weco.catalogue.internal_model.work.{Work, WorkState}

trait CatalogueJsonUtil {
  implicit class WorkOps(w: Work.Visible[WorkState.Indexed]) {
    def asJson(includes: WorksIncludes): Json =
      DisplayWork(w, includes).asJson
  }

  implicit class ImageOps(im: Image[ImageState.Indexed]) {
    def asJson(includes: MultipleImagesIncludes): Json =
      DisplayImage(im, includes).asJson
  }
}
