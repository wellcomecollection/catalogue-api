package weco.api.search.json

import io.circe.Json
import io.circe.syntax._
import weco.api.search.models.index.IndexedImage
import weco.api.search.models.request._

trait CatalogueJsonUtil {
  import JsonOps._

  implicit class WorkJsonOps(json: Json) {

    /** Remove any fields from this JSON which aren't explicitly "include"-d
      * in the user's request.
      *
      * We remove identifiers recursively because they appear at multiple levels
      * in a Work (e.g. on the work itself and on items).
      *
      * We only remove the remaining fields on the top level because that's
      * where we expect them to appear, e.g. we don't expect "holdings" to be
      * anywhere but on the work itself.
      *
      */
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

  implicit class ImageJsonOps(json: Json) {
    def asJson(
      includes: SingleImageIncludes,
      visuallySimilar: Option[Seq[IndexedImage]],
      withSimilarColors: Option[Seq[IndexedImage]],
      withSimilarFeatures: Option[Seq[IndexedImage]]
    ): Json =
      json
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
        .withIncludes(includes)
        .deepDropNullValues

    def addImagesIf[V](
      b: Boolean,
      key: String,
      value: Option[Seq[IndexedImage]]
    ): Json =
      if (b)
        json.mapObject(
          jsonObj =>
            value match {
              case Some(v) => jsonObj.add(key, v.map(_.display).asJson)
              case None    => jsonObj
            }
        )
      else
        json

    def withIncludes(includes: ImageIncludes): Json =
      json
        .removeKeyRecursivelyIf(!includes.`source.contributors`, "contributors")
        .removeKeyRecursivelyIf(!includes.`source.genres`, "genres")
        .removeKeyRecursivelyIf(!includes.`source.languages`, "languages")
        .removeKeyRecursivelyIf(!includes.`source.subjects`, "subjects")
  }

  implicit class ImplicitCatalogueJsonOps(j: Json) {
    def removeKeyRecursivelyIf(b: Boolean, key: String): Json =
      if (b) j.removeKeyRecursively(key) else j

    def removeKeyIf(b: Boolean, key: String): Json =
      if (b) j.removeKey(key) else j
  }
}
