package weco.api.search.images

import weco.api.search.ApiTestBase
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.SingleImageIncludes
import weco.catalogue.display_model.test.util.DisplaySerialisationTestBase
import weco.catalogue.internal_model.generators.ImageGenerators
import weco.catalogue.internal_model.image._

trait ApiImagesTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with ImageGenerators
    with CatalogueJsonUtil
    with TestDocumentFixtures {

  def singleImageResult: String =
    s"""
       |  "type": "Image"
     """.stripMargin

  def imageSource(source: ImageSource): String =
    source match {
      case ParentWorks(work, _) =>
        s"""
           |{
           |  "id": "${source.id.canonicalId}",
           |  "title": ${work.data.title.map(_.toJson)},
           |  "type": "Work"
           |}
           |""".stripMargin.tidy
    }

  def imageResponse(image: Image[ImageState.Indexed]): String =
    s"""
       |  {
       |    "type": "Image",
       |    "id": "${image.id}",
       |    "thumbnail": ${location(image.state.derivedData.thumbnail)},
       |    "locations": [${locations(image.locations)}],
       |    "source": ${imageSource(image.source)}
       |  }
     """.stripMargin

  def imagesListResponse(images: Seq[Image[ImageState.Indexed]]): String =
    s"""
       |{
       |  ${resultList(
         totalResults = images.size,
         totalPages = if (images.nonEmpty) {
           1
         } else {
           0
         }
       )},
       |  "results": [
       |    ${images.map(imageResponse).mkString(",")}
       |  ]
       |}
    """.stripMargin

  def newImagesListResponse(
    ids: Seq[String],
    strictOrdering: Boolean = false
  ): String = {
    val works = ids.map { getDisplayImage }.map {
      _.withIncludes(SingleImageIncludes.none)
    }

    val sortedWorks = if (strictOrdering) {
      works
    } else {
      works.sortBy(w => getKey(w, "id").get.asString)
    }

    s"""
       |{
       |  ${resultList(totalResults = ids.size)},
       |  "results": [
       |    ${sortedWorks.mkString(",")}
       |  ]
       |}
      """.stripMargin
  }
}
