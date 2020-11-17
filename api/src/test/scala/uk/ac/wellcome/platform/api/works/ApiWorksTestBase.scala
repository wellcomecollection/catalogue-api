package uk.ac.wellcome.platform.api.works
import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators._
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.api.ApiTestBase
import WorkState.Derived

trait ApiWorksTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with WorkGenerators
    with GenreGenerators
    with SubjectGenerators {

  implicit object IdentifiedWorkIndexable
      extends Indexable[Work.Visible[Derived]] {
    override def json(work: Work.Visible[Derived]): String =
      toJson(work).get
  }

  def singleWorkResult(apiPrefix: String,
                       ontologyType: String = "Work"): String =
    s"""
        "@context": "${contextUrl(apiPrefix)}",
        "type": "$ontologyType"
     """.stripMargin

  def workResponse(work: Work.Visible[Derived]): String =
    s"""
      | {
      |   "type": "${formatOntologyType(work.data.workType)}",
      |   "id": "${work.state.canonicalId}",
      |   "title": "${work.data.title.get}",
      |   "availableOnline": ${hasDigitalLocations(work)},
      |   "alternativeTitles": []
      |   ${optionalObject("workType", format, work.data.format)}
      |   ${optionalObject("language", language, work.data.language)}
      | }
    """.stripMargin

  def worksListResponse(apiPrefix: String,
                        works: Seq[Work.Visible[Derived]]): String =
    s"""
       |{
       |  ${resultList(apiPrefix, totalResults = works.size)},
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

  def hasDigitalLocations(work: Work.Visible[Derived]): String =
    work.data.items
      .exists(_.locations.exists(_.isInstanceOf[DigitalLocationDeprecated]))
      .toString
}
