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

    val queryables: Map[String, Json] = aggregables.map {
      case (key, values) => key -> values.asJson
    } + ("title" -> title.asJson)

    val aggregablesJson = aggregables.map {
      case (key, values) =>
        key -> values.map(toAggregable).asJson
    } asJson

    val doc = Json.obj(
      "display" -> Json.obj("id" -> docId.asJson, "title" -> title.asJson),
      "type" -> Json.fromString("Visible"),
      "query" -> queryables.asJson,
      "aggregatableValues" -> aggregablesJson
    )

    TestDocument(
      docId,
      doc
    )
  }

  def createImageDocument(
    docId: String,
    title: String = "",
    aggregables: Map[String, Seq[String]]
  ): TestDocument = {

    val queryables: Map[String, Json] = aggregables.map {
      case (key, values) => key -> values.asJson
    } + ("source.title" -> title.asJson) + ("inferredData.reducedFeatures" -> reducedFeaturePlaceholder.asJson)

    val aggregablesJson = aggregables.map {
      case (key, values) =>
        key -> values.map(toAggregable).asJson
    } asJson

    val doc = Json.obj(
      "display" -> Json.obj("id" -> docId.asJson, "title" -> title.asJson),
      "query" -> queryables.asJson,
      "aggregatableValues" -> aggregablesJson
    )

    TestDocument(
      docId,
      doc
    )
  }

  private def toAggregable(value: String): String =
    s"""{"label":${value.asJson}}"""

  private val reducedFeaturePlaceholder: Seq[Float] =
    normalize(Seq.fill(1024)(random.nextGaussian().toFloat))

  private def norm(vec: Seq[Float]): Float =
    math.sqrt(vec.fold(0.0f)((total, i) => total + (i * i))).toFloat

  private def normalize(vec: Seq[Float]): Seq[Float] =
    scalarMultiply(1 / norm(vec), vec)

  private def scalarMultiply(a: Float, vec: Seq[Float]): Seq[Float] =
    vec.map(_ * a)

}
