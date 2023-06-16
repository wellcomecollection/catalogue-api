package weco.api.search.images

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.ApiTestBase
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.SingleImageIncludes

trait ApiImagesTestBase
    extends AnyFunSpec
    with ApiTestBase
    with CatalogueJsonUtil
    with TestDocumentFixtures {

  def imagesListResponse(
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
