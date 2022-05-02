package weco.api.search.json

import io.circe.Json
import io.circe.syntax._
import weco.api.search.models.request._
import weco.catalogue.display_model.Implicits._
import weco.catalogue.display_model.image.DisplayImage
import weco.catalogue.display_model.work.DisplayWork
import weco.catalogue.internal_model.image.{Image, ImageState}
import weco.catalogue.internal_model.work.{Work, WorkState}

trait CatalogueJsonUtil {
  import JsonOps._

  implicit class WorkJsonOps(json: Json) {
    def withIncludes(includes: WorksIncludes): Json =
      json
        .removeKeyRecursivelyIf(!includes.identifiers, "identifiers")
        .removeKeyIf(!includes.items, "items")
        .removeKeyIf(!includes.holdings, "holdings")
        .removeKeyIf(!includes.subjects, "subjects")
        .removeKeyIf(!includes.genres, "genres")
        .removeKeyIf(!includes.contributors, "contributors")
        .removeKeyIf(!includes.production, "production")
        .removeKeyIf(!includes.languages, "languages")
        .removeKeyIf(!includes.notes, "notes")
        .removeKeyIf(!includes.images, "images")
        .removeKeyIf(!includes.parts, "parts")
        .removeKeyIf(!includes.partOf, "partOf")
        .removeKeyIf(!includes.precededBy, "precededBy")
        .removeKeyIf(!includes.succeededBy, "succeededBy")
  }

  implicit class WorkOps(w: Work.Visible[WorkState.Indexed]) {
    def asJson(includes: WorksIncludes): Json =
      DisplayWork(w).asJson
        .deepDropNullValues
        .withIncludes(includes)
  }

  implicit class ImageOps(im: Image[ImageState.Indexed]) {
    def asJson(includes: MultipleImagesIncludes): Json =
      DisplayImage(im).asJson
        .withIncludes(includes)

    def asJson(
      includes: SingleImageIncludes,
      visuallySimilar: Option[Seq[Image[ImageState.Indexed]]],
      withSimilarColors: Option[Seq[Image[ImageState.Indexed]]],
      withSimilarFeatures: Option[Seq[Image[ImageState.Indexed]]]
    ): Json = {
      val baseJson =
        DisplayImage(im).asJson
          .addImagesIf(
            includes.visuallySimilar,
            key = "visuallySimilar",
            value = visuallySimilar
          )
          .addImagesIf(
            includes.withSimilarColors,
            key = "withSimilarColors",
            value = withSimilarColors
          )
          .addImagesIf(
            includes.withSimilarFeatures,
            key = "withSimilarFeatures",
            value = withSimilarFeatures
          )

      baseJson.withIncludes(includes)
    }
  }

  implicit class ImplicitCatalogueJsonOps(j: Json) {
    def removeKeyRecursivelyIf(b: Boolean, key: String): Json =
      if (b) j.removeKeyRecursively(key) else j

    def removeKeyIf(b: Boolean, key: String): Json =
      if (b) j.removeKey(key) else j

    def addImagesIf[V](
      b: Boolean,
      key: String,
      value: Option[Seq[Image[ImageState.Indexed]]]
    ): Json =
      if (b)
        j.mapObject(
          jsonObj =>
            value match {
              case Some(v) => jsonObj.add(key, v.map(DisplayImage(_)).asJson)
              case None    => jsonObj
            }
        )
      else
        j

    def withIncludes(includes: ImageIncludes): Json =
      j.removeKeyRecursivelyIf(!includes.`source.contributors`, "contributors")
        .removeKeyRecursivelyIf(!includes.`source.genres`, "genres")
        .removeKeyRecursivelyIf(!includes.`source.languages`, "languages")
  }
}
