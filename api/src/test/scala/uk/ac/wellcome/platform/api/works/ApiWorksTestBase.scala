package uk.ac.wellcome.platform.api.works
import com.sksamuel.elastic4s.Indexable
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators._
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.api.ApiTestBase
import WorkState.Identified

trait ApiWorksTestBase
    extends ApiTestBase
    with DisplaySerialisationTestBase
    with WorkGenerators
    with GenreGenerators
    with SubjectGenerators {

  implicit object IdentifiedWorkIndexable
      extends Indexable[Work.Visible[Identified]] {
    override def json(t: Work.Visible[Identified]): String =
      toJson(t).get
  }

  def singleWorkResult(apiPrefix: String,
                       ontologyType: String = "Work"): String =
    s"""
        "@context": "${contextUrl(apiPrefix)}",
        "type": "$ontologyType"
     """.stripMargin

  def workResponse(work: Work.Visible[Identified]): String =
    s"""
      | {
      |   "type": "${formatOntologyType(work.data.workType)}",
      |   "id": "${work.state.canonicalId}",
      |   "title": "${work.data.title.get}",
      |   ${work.data.format.map(formatResponse).getOrElse("")}
      |   ${work.data.language.map(languageResponse).getOrElse("")}
      |   "alternativeTitles": []
      | }
    """.stripMargin

  def worksListResponse(apiPrefix: String,
                        works: Seq[Work.Visible[Identified]]): String =
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

  def formatResponse(format: Format): String =
    s"""
      | "workType": {
      |   "id": "${format.id}",
      |   "label": "${format.label}",
      |   "type": "Format"
      | },
    """.stripMargin

  def languageResponse(language: Language): String =
    s"""
      | "language": {
      |   ${language.id.map(lang => s""""id": "$lang",""").getOrElse("")}
      |   "label": "${language.label}",
      |   "type": "Language"
      | },
    """.stripMargin
}
