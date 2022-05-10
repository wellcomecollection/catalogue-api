package weco.api.search.works

import weco.api.search.ApiTestBase
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.request.WorksIncludes
import weco.catalogue.display_model.test.util.DisplaySerialisationTestBase

trait ApiWorksTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with CatalogueJsonUtil
    with TestDocumentFixtures {
//
//  implicit object IdentifiedWorkIndexable
//      extends Indexable[Work.Visible[Indexed]] {
//    override def json(work: Work.Visible[Indexed]): String =
//      toJson(work).get
//  }

  def worksListResponse(
    ids: Seq[String],
    includes: WorksIncludes = WorksIncludes.none,
    strictOrdering: Boolean = false
  ): String = {
    val works =
      ids
        .map { getVisibleWork }
        .map(_.display.withIncludes(includes))

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
