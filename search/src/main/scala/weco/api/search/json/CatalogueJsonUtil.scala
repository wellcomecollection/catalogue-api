package weco.api.search.json

import io.circe.Json
import io.circe.syntax._
import weco.catalogue.display_model.models.Implicits._
import weco.catalogue.display_model.models._
import weco.catalogue.internal_model.image.{Image, ImageState}
import weco.catalogue.internal_model.work.{Work, WorkState}

trait CatalogueJsonUtil {
  import JsonOps._

  implicit class WorkOps(w: Work.Visible[WorkState.Indexed]) {
    def asJson(includes: WorksIncludes): Json =
      DisplayWork(w).asJson
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
        .deepDropNullValues
  }

  implicit class ImageOps(im: Image[ImageState.Indexed]) {
    def asJson(includes: MultipleImagesIncludes): Json =
      DisplayImage(im, includes).asJson

    def asJson(
      includes: SingleImageIncludes,
      visuallySimilar: Option[Seq[Image[ImageState.Indexed]]],
      withSimilarColors: Option[Seq[Image[ImageState.Indexed]]],
      withSimilarFeatures: Option[Seq[Image[ImageState.Indexed]]]
    ): Json = {
      val baseJson =
        DisplayImage(im, includes).asJson
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

      baseJson
        .removeKeyRecursivelyIf(!includes.`source.contributors`, "contributors")
        .removeKeyRecursivelyIf(!includes.`source.genres`, "genres")
        .removeKeyRecursivelyIf(!includes.`source.languages`, "languages")
    }
  }

  implicit class ImplicitCatalogueJsonOps(j: Json) {
    def removeKeyRecursivelyIf(b: Boolean, key: String): Json =
      if (b) j.removeKeyRecursively(key) else j

    def removeKeyIf(b: Boolean, key: String): Json =
      if (b) j.removeKey(key) else j

    def addImagesIf[V](b: Boolean, key: String, value: Option[Seq[Image[ImageState.Indexed]]]): Json =
      if (b)
        j.mapObject(jsonObj =>
          value match {
            case Some(v) => jsonObj.add(key, value.map(images => images.map(DisplayImage(_, SingleImageIncludes.all))).asJson)
            case None    => jsonObj
          }
        )
      else
        j
  }
}
