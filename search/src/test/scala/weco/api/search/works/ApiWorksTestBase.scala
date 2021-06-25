package weco.api.search.works

import com.sksamuel.elastic4s.Indexable
import weco.api.search.ApiTestBase
import weco.catalogue.internal_model.Implicits._
import weco.json.JsonUtil._
import weco.catalogue.internal_model.work.generators._
import weco.catalogue.display_model.models.DisplaySerialisationTestBase
import weco.catalogue.internal_model.locations.DigitalLocation
import weco.catalogue.internal_model.work.{Work, WorkType}
import weco.catalogue.internal_model.work.WorkState.Indexed

trait ApiWorksTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with WorkGenerators
    with GenreGenerators
    with SubjectGenerators {

  implicit object IdentifiedWorkIndexable
      extends Indexable[Work.Visible[Indexed]] {
    override def json(work: Work.Visible[Indexed]): String =
      toJson(work).get
  }

  def singleWorkResult(ontologyType: String = "Work"): String =
    s"""
        "@context": "$contextUrl",
        "type": "$ontologyType"
     """.stripMargin

  def workResponse(work: Work.Visible[Indexed]): String =
    s"""
      | {
      |   "type": "${formatOntologyType(work.data.workType)}",
      |   "id": "${work.state.canonicalId}",
      |   "title": "${work.data.title.get}",
      |   "availabilities": [${availabilities(work.state.availabilities)}],
      |   "alternativeTitles": []
      |   ${optionalObject("workType", format, work.data.format)}
      | }
    """.stripMargin

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