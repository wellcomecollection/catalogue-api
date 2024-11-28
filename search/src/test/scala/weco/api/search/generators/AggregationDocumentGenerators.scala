package weco.api.search.generators
import io.circe.Json
import io.circe.syntax._
import weco.api.search.fixtures.TestDocumentFixtures

trait AggregationDocumentGenerators {
  this: TestDocumentFixtures =>

  def createWorkDocument(
    docId: String,
    title: String = "",
    aggregables: Map[String, Seq[Json]],
    filterables: Map[String, Json]
  ): TestDocument = {
    val queryables = filterables.filterKeys {
      case "contributors.agent.label" => true
      case "genres.label" => true
      case "subjects.label" => true
      case _ => false
    } + ("title" -> title.asJson)

    val doc = Json.obj(
      "display" -> Json.obj("id" -> docId.asJson, "title" -> title.asJson),
      "type" -> Json.fromString("Visible"),
      "query" -> queryables.asJson,
      "aggregatableValues" -> aggregables.asJson,
      "filterableValues" -> filterables.asJson
    )

    TestDocument(
      docId,
      doc
    )
  }

  def createAggregatableField(id: String, label: String): Json = {
    Json.obj(
      "id" -> id.asJson,
      "label" -> label.asJson
    )
  }

  def createAggregatableField(label: String): Json = {
    createAggregatableField(label, label)
  }

  def createImageDocument(
    docId: String,
    title: String = "",
    aggregables: Map[String, Seq[Json]],
    filterables: Map[String, Json]
  ): TestDocument = {
    val queryables = filterables.filterKeys {
      case "source.contributors.agent.label" => true
      case "source.genres.concepts.label" => true
      case "source.subjects.concepts.label" => true
      case _ => false
    } + ("source.title" -> title.asJson)

    val doc = Json.obj(
      "display" -> Json.obj("id" -> docId.asJson, "title" -> title.asJson),
      "query" -> queryables.asJson,
      "aggregatableValues" -> aggregables.asJson,
      "filterableValues" -> filterables.asJson,
      "vectorValues" -> Json.obj(
        "features" -> featuresPlaceholder.asJson
      )
    )

    TestDocument(
      docId,
      doc
    )
  }

  private val featuresPlaceholder: Seq[Float] =
    normalize(Seq.fill(4096)(random.nextGaussian().toFloat))

  private def norm(vec: Seq[Float]): Float =
    math.sqrt(vec.fold(0.0f)((total, i) => total + (i * i))).toFloat

  private def normalize(vec: Seq[Float]): Seq[Float] =
    scalarMultiply(1 / norm(vec), vec)

  private def scalarMultiply(a: Float, vec: Seq[Float]): Seq[Float] =
    vec.map(_ * a)

}
