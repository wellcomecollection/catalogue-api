package weco.api.search.images

import weco.api.search.ApiTestBase
import weco.catalogue.display_model.models.DisplaySerialisationTestBase
import weco.catalogue.internal_model.generators.ImageGenerators
import weco.catalogue.internal_model.image._

trait ApiImagesTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with ImageGenerators {

  def singleImageResult: String =
    s"""
       |  "@context": "$contextUrl",
       |  "type": "Image"
     """.stripMargin

  def imageSource(source: ImageSource): String =
    source match {
      case ParentWorks(work, _) =>
        s"""
             | {
             |   "id": "${source.id.canonicalId}",
             |   ${optionalString("title", work.data.title)}
             |   "type": "Work"
             | }
           """.stripMargin
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
}
