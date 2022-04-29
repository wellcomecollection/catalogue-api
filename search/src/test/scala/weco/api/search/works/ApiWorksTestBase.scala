package weco.api.search.works

import com.sksamuel.elastic4s.Indexable
import weco.api.search.{ApiTestBase, JsonHelpers}
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.json.CatalogueJsonUtil
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.request.WorksIncludes
import weco.catalogue.display_model.test.util.DisplaySerialisationTestBase
import weco.catalogue.internal_model.Implicits._
import weco.json.JsonUtil._
import weco.catalogue.internal_model.work.generators._
import weco.catalogue.internal_model.locations.DigitalLocation
import weco.catalogue.internal_model.work.{Work, WorkType}
import weco.catalogue.internal_model.work.WorkState.Indexed

trait ApiWorksTestBase
    extends ApiTestBase
    with TestDocumentFixtures
    with DisplaySerialisationTestBase
    with WorkGenerators
    with GenreGenerators
    with SubjectGenerators
    with CatalogueJsonUtil
    with JsonHelpers {

  implicit object IdentifiedWorkIndexable
      extends Indexable[Work.Visible[Indexed]] {
    override def json(work: Work.Visible[Indexed]): String =
      toJson(work).get
  }

  def singleWorkResult(ontologyType: String = "Work"): String =
    s"""
        "type": "$ontologyType"
     """.stripMargin

  def workResponse(work: Work.Visible[Indexed]): String =
    s"""
      | {
      |   "id": "${work.state.canonicalId}",
      |   "title": "${work.data.title.get}",
      |   "availabilities": [${availabilities(work.state.availabilities)}],
      |   "alternativeTitles": [],
      |   "workType": ${work.data.format.map(format)},
      |   "type": "${formatOntologyType(work.data.workType)}"
      | }
    """.stripMargin.tidy

  def getTestWork(id: String): IndexedWork.Visible = {
    val doc = getTestDocuments(Seq(id)).head
    doc.document.as[IndexedWork.Visible].right.get
  }

  def newWorksListResponse(ids: Seq[String], sortByCanonicalId: Boolean = true): String = {
    val works = ids
      .map(getTestWork)
      .sortBy { w =>
        if (sortByCanonicalId)
          getKey(w.display, "id").get.toString
        else
          "0"
      }
    val displayWorks = works.map(_.display.withIncludes(WorksIncludes.none).noSpaces)
    s"""
       |{
       |  ${resultList(totalResults = works.size)},
       |  "results": [ ${displayWorks.mkString(",")} ]
       |}
       |""".stripMargin
  }

  def worksListResponse(works: Seq[Work.Visible[Indexed]]): String =
    s"""
       |{
       |  ${resultList(totalResults = works.size)},
       |  "results": [
       |    ${works.map { workResponse }.mkString(",")}
       |  ]
       |}
      """.stripMargin

  def formatOntologyType(workType: WorkType): String =
    workType match {
      case WorkType.Standard   => "Work"
      case WorkType.Collection => "Collection"
      case WorkType.Series     => "Series"
      case WorkType.Section    => "Section"
    }

  def hasDigitalLocations(work: Work.Visible[Indexed]): String =
    work.data.items
      .exists(_.locations.exists(_.isInstanceOf[DigitalLocation]))
      .toString
}
