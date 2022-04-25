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
  import JsonOps._

  implicit class WorkOps(w: Work.Visible[WorkState.Indexed]) {
    def asJson(includes: WorksIncludes): Json =
      DisplayWork(w).asJson
        .removeKeyRecursivelyIf(includes.identifiers, "identifiers")
        .removeKeyIf(includes.items, "items")
        .removeKeyIf(includes.holdings, "holdings")
        .removeKeyIf(includes.subjects, "subjects")
        .removeKeyIf(includes.genres, "genres")
        .removeKeyIf(includes.contributors, "contributors")
        .removeKeyIf(includes.production, "production")
        .removeKeyIf(includes.languages, "languages")
        .removeKeyIf(includes.notes, "notes")
        .removeKeyIf(includes.images, "images")
        .removeKeyIf(includes.parts, "parts")
        .removeKeyIf(includes.partOf, "partOf")
        .removeKeyIf(includes.precededBy, "precededBy")
        .removeKeyIf(includes.succeededBy, "succeededBy")
        .deepDropNullValues
  }

  implicit class ImageOps(im: Image[ImageState.Indexed]) {
    def asJson(includes: MultipleImagesIncludes): Json =
      DisplayImage(im, includes).asJson
  }

  implicit class ImplicitCatalogueJsonOps(j: Json) {
    def removeKeyRecursivelyIf(b: Boolean, key: String): Json =
      if (!b) j.removeKeyRecursively(key) else j

    def removeKeyIf(b: Boolean, key: String): Json =
      if (!b) j.removeKey(key) else j
  }
}
