package weco.api.search.works

import com.sksamuel.elastic4s.Indexable
import weco.api.search.ApiTestBase
import weco.catalogue.display_model.test.util.DisplaySerialisationTestBase
import weco.catalogue.internal_model.Implicits._
import weco.json.JsonUtil._
import weco.catalogue.internal_model.work.generators._
import weco.catalogue.internal_model.locations.DigitalLocation
import weco.catalogue.internal_model.work.{Work, WorkType}
import weco.catalogue.internal_model.work.WorkState.Indexed

trait ApiWorksTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with WorkGenerators
    with GenreGenerators
    with SubjectGenerators {

  val listOfWorks = List(
    "works.visible.0",
    "works.visible.1",
    "works.visible.2",
    "works.visible.3",
    "works.visible.4",
    "works.invisible.0",
    "works.invisible.1",
    "works.invisible.2",
    "works.redirected.0",
    "works.redirected.1",
    "works.deleted.0",
    "works.deleted.1",
    "works.deleted.2",
    "works.deleted.3"
  )

  val formatWorks = List("works.formats.0.Books", "works.formats.1.Books", "works.formats.2.Books", "works.formats.3.Books",
    "works.formats.4.Journals","works.formats.5.Journals","works.formats.6.Journals",
    "works.formats.7.Audio","works.formats.8.Audio","works.formats.9.Pictures")

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
