package weco.api.search.generators
import io.circe.Json
import io.circe.syntax._
import weco.api.search.fixtures.TestDocumentFixtures

trait AggregationDocumentGenerators {
  this: TestDocumentFixtures =>

  def createWorkDocument(
    docId: String,
    title: String = "",
    aggregables: Map[String, Seq[String]]
  ): TestDocument = {

    val queryables = aggregables map {
      case (key, values) =>
        Json.obj(key -> {
          values.asJson
        })
    } asJson

    val aggregablesJson = aggregables.map {
      case (key, values) =>
        Json.obj(key -> values.map(toAggregable).asJson)
    } asJson

    val doc = Json.obj(
      "display" -> Json.obj("id" -> docId.asJson, "title" -> title.asJson),
      "type" -> Json.fromString("Visible"),
      "query" -> queryables,
      "aggregatableValues" -> aggregablesJson
    )

    TestDocument(
      docId,
      doc
    )
  }

  private def toAggregable(value: String): String =
    s"""{"label":${value.asJson}}"""

}
