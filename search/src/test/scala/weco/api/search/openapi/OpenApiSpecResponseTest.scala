package weco.api.search.openapi

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersion}
import io.circe.Json
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import scala.collection.JavaConverters._
import scala.io.Source

/** Checks the spec's response schemas against the documents the API actually returns.
  *
  * Both services serve a pre-rendered `display` document straight out of
  * Elasticsearch, so these schemas cannot be generated from this repo. They can be
  * checked, though. The pipeline generates the documents in
  * common/search/src/test/resources/test_documents, and copy_test_documents.py copies
  * them here. The `document.display` object in each one is exactly what the API
  * returns for that record.
  *
  * So this validates every one of them against the `Work` or `Image` schema. If the
  * pipeline changes the display model and someone refreshes the fixtures, this fails
  * until the spec catches up.
  *
  * Two limits worth knowing. The fixtures are a sample, not every field the pipeline
  * can emit, so this proves the schema accepts what we have rather than everything
  * that exists. And because the copy is manual, a pipeline change is only caught once
  * someone re-runs copy_test_documents.py.
  */
class OpenApiSpecResponseTest extends AnyFunSpec with Matchers {

  private val mapper = new ObjectMapper()

  private val specNode: JsonNode =
    mapper.readTree(OpenApiSpec.parsed.noSpaces)

  /** OpenAPI 3.1 schemas are JSON Schema 2020-12, so they validate directly. The
    * `$ref`s point at `#/components/schemas/...`, so `components` has to travel with
    * the schema for them to resolve.
    */
  private def schemaFor(name: String): JsonSchema = {
    val root = mapper.createObjectNode()
    root.put("$ref", s"#/components/schemas/$name")
    root.set("components", specNode.get("components"))

    JsonSchemaFactory
      .getInstance(SpecVersion.VersionFlag.V202012)
      .getSchema(root)
  }

  private lazy val workSchema = schemaFor("Work")
  private lazy val imageSchema = schemaFor("Image")

  private def testDocuments: Seq[File] =
    OpenApiSpec
      .repoFile("common/search/src/test/resources/test_documents")
      .listFiles()
      .filter(_.getName.endsWith(".json"))
      .sortBy(_.getName)

  /** Redirected, invisible and deleted works carry no display document. */
  private def displayOf(file: File): Option[JsonNode] =
    Option(mapper.readTree(file).at("/document/display"))
      .filterNot(_.isMissingNode)

  /** Collection, Series and Section are works too; only the `type` differs. */
  private def schemaOf(display: JsonNode): Option[(String, JsonSchema)] =
    display.at("/type").asText() match {
      case "Image" => Some("Image" -> imageSchema)
      case "Work" | "Collection" | "Series" | "Section" =>
        Some("Work" -> workSchema)
      case _ => None
    }

  private def errorsFor(schema: JsonSchema, node: JsonNode): Seq[String] =
    schema.validate(node).asScala.toSeq.map(_.getMessage)

  describe("the response schemas") {
    it("accept every display document the pipeline generates") {
      val failures = testDocuments.flatMap { file =>
        for {
          display <- displayOf(file)
          (name, schema) <- schemaOf(display)
          errors = errorsFor(schema, display)
          if errors.nonEmpty
        } yield
          s"${file.getName} [$name]\n    ${errors.take(3).mkString("\n    ")}"
      }

      withClue(
        s"""|These display documents do not match the spec's response schemas.
            |Either the schema is wrong, or the pipeline's display model has moved.
            |
            |${failures.mkString("\n  ")}
            |""".stripMargin
      ) {
        failures shouldBe empty
      }
    }

    it("find documents to check") {
      // A negative control: without this, the assertion above would pass if
      // testDocuments or displayOf silently returned nothing.
      val checked = testDocuments.flatMap(displayOf).flatMap(schemaOf)

      checked.count(_._1 == "Work") should be > 100
      checked.count(_._1 == "Image") should be > 20
    }

    it("reject a display document with the wrong shape") {
      // A second control: the validator must actually reject something.
      val broken = mapper.readTree("""{"id": 12345, "type": "Work"}""")

      errorsFor(workSchema, broken) should not be empty
    }
  }

  /** The documents above are single works and images. These fixtures are whole API
    * responses, so they cover the envelope: pageSize, totalPages, results, and so on.
    *
    * They are generated from the same test documents by
    * expected_responses/generate_works_includes_responses.py, so they are not
    * independent evidence of the field shapes. The envelope is the point.
    */
  describe("the result list schemas") {
    it("accept the expected responses the tests assert on") {
      val fixtures = OpenApiSpec
        .repoFile("search/src/test/resources/expected_responses")
        .listFiles()
        .filter(_.getName.endsWith(".json"))
        .sortBy(_.getName)

      fixtures.length should be > 30

      val failures = fixtures.flatMap { file =>
        val node = mapper.readTree(file)
        val errors = errorsFor(schemaFor(envelopeSchemaName(node)), node)

        if (errors.isEmpty) None
        else Some(s"${file.getName}\n    ${errors.take(3).mkString("\n    ")}")
      }

      withClue(s"\n  ${failures.mkString("\n  ")}\n") {
        failures shouldBe empty
      }
    }
  }

  /** A fixture is either a bare Work or Image, or a ResultList of them. */
  private def envelopeSchemaName(node: JsonNode): String =
    node.at("/type").asText() match {
      case "ResultList" =>
        val first = node.at("/results/0/type").asText()
        if (first == "Image") "ImageResultList" else "WorkResultList"
      case "Image" => "Image"
      case _       => "Work"
    }

  /** JSON Schema ignores properties it doesn't know about, so the validation above
    * says nothing about a field the pipeline emits and we never wrote down. This walks
    * the documents instead, and reports any key with no matching property in the
    * schema.
    */
  describe("the documented fields") {
    it("cover every field the pipeline's display documents contain") {
      val failures = testDocuments.flatMap { file =>
        val doc = circeOf(file)

        for {
          display <- doc.hcursor
            .downField("document")
            .downField("display")
            .focus
          name <- schemaNameOf(display)
          missing = undocumentedFields(schemaRef(name), display, name)
          if missing.nonEmpty
        } yield s"${file.getName}: ${missing.distinct.mkString(", ")}"
      }

      withClue(
        s"""|These fields appear in the pipeline's display documents but are not in the
            |spec. Either document them, or confirm the API strips them before it
            |responds.
            |
            |${failures.mkString("\n  ")}
            |""".stripMargin
      ) {
        failures shouldBe empty
      }
    }

    it("notice a field that is not documented") {
      // A control: drop a property from the schema and the walk must report it.
      val work = schemaRef("Work")
      val withoutTitle = work.hcursor
        .downField("properties")
        .downField("title")
        .delete
        .top
        .get

      val display = Json.obj("title" -> Json.fromString("Anything"))

      undocumentedFields(withoutTitle, display, "Work") should contain(
        "Work.title")
    }
  }

  /** The walk above catches a field the pipeline added and we never documented. This
    * is the reverse: a field the spec documents should appear in at least one fixture,
    * or it is evidence of a removal the spec never caught up with (or a field that was
    * never real).
    *
    * Fields are compared per component, not per position: `Identifier.value` counts
    * as exercised wherever an identifier appears, so a fixture doesn't need to carry
    * identifiers on every entity that can hold them. Some documented fields are
    * genuinely rare and the generated sample doesn't reach them at all; those are
    * allowed by name below, so a removal of anything else fails here.
    */
  describe("the fixture coverage") {
    it("exhibits every field the spec documents") {
      val observed = testDocuments.flatMap { file =>
        val doc = circeOf(file)

        for {
          display <- doc.hcursor
            .downField("document")
            .downField("display")
            .focus
            .toSeq
          name <- schemaNameOf(display).toSeq
          path <- observedFields(schemaRef(name), display, name)
        } yield path
      }.toSet

      val missing = documentedFields -- observed -- allowedUnexercised

      withClue(
        s"""|The spec documents these fields, but no fixture contains them. Either the
            |pipeline no longer emits them and the spec should drop them, or they are
            |rare and belong in allowedUnexercised with a note.
            |
            |${missing.toSeq.sorted.mkString("\n  ")}
            |""".stripMargin
      ) {
        missing shouldBe empty
      }
    }

    it("does not allow fields the fixtures exercise") {
      // Entries here would mask a regression the assertion above exists to catch.
      val observed = testDocuments.flatMap { file =>
        val doc = circeOf(file)

        for {
          display <- doc.hcursor
            .downField("document")
            .downField("display")
            .focus
            .toSeq
          name <- schemaNameOf(display).toSeq
          path <- observedFields(schemaRef(name), display, name)
        } yield path
      }.toSet

      allowedUnexercised.intersect(observed) shouldBe empty
    }

    it("notices a documented field that no fixture contains") {
      // A control: an invented property must show up in the documented set.
      documentedFieldsFrom(
        "Work",
        name =>
          if (name != "Work") schemaRef(name)
          else {
            val work = schemaRef("Work")
            work.hcursor
              .downField("properties")
              .focus
              .flatMap(_.asObject)
              .map(
                _.add(
                  "neverEmitted",
                  Json.obj("type" -> Json.fromString("string"))))
              .map(Json.fromJsonObject)
              .flatMap(
                props =>
                  work.hcursor
                    .downField("properties")
                    .set(props)
                    .top
              )
              .get
          }
      ) should contain("Work.neverEmitted")
    }
  }

  /** Documented fields with no fixture yet. Each needs a reason; an entry the fixtures
    * do exercise fails the guard test above, so this list cannot rot silently.
    */
  private lazy val allowedUnexercised: Set[String] = Set(
    // Spliced in by the API at request time, never part of an indexed document.
    "Image.withSimilarFeatures",
    // Real fields the deterministic sample never populates. Teaching the pipeline's
    // document generators about them would let these come off the list.
    "AccessCondition.note",
    "AccessCondition.terms",
    "Agent.identifiers",
    "Item.note",
    "Item.title",
    "Period.id",
    "Period.identifiers",
    "Place.id",
    "Place.identifiers",
    "ProductionEvent.function",
    "Work.createdDate",
    "Work.currentFrequency",
    "Work.physicalDescription",
    "Work.referenceNumber"
  )

  /** Every property of every component reachable from the Work and Image schemas, as
    * `Component.property` paths; inline sub-objects extend the dotted path instead.
    */
  private lazy val documentedFields: Set[String] =
    documentedFieldsFrom("Work", schemaRef) ++
      documentedFieldsFrom("Image", schemaRef)

  private def refNameOf(schema: Json): Option[String] =
    schema.hcursor
      .get[String]("$ref")
      .toOption
      .map(_.stripPrefix("#/components/schemas/"))

  private def documentedFieldsFrom(
    root: String,
    componentSchema: String => Json
  ): Set[String] = {
    val visited = scala.collection.mutable.Set(root)
    val queue = scala.collection.mutable.Queue(root)
    val paths = scala.collection.mutable.Set.empty[String]

    // A $ref starts its own component walk; everything inline extends the path.
    def follow(schema: Json, path: String): Unit =
      refNameOf(schema) match {
        case Some(name) => if (visited.add(name)) queue += name
        case None       => walkInline(schema, path)
      }

    def walkInline(schema: Json, path: String): Unit = {
      schema.hcursor
        .downField("oneOf")
        .focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)
        .foreach(follow(_, path))

      schema.hcursor
        .downField("properties")
        .focus
        .flatMap(_.asObject)
        .foreach(_.toIterable.foreach {
          case (key, sub) =>
            paths += s"$path.$key"
            follow(sub, s"$path.$key")
        })

      schema.hcursor
        .downField("items")
        .focus
        .foreach(follow(_, s"$path[]"))
    }

    while (queue.nonEmpty) {
      val name = queue.dequeue()
      walkInline(componentSchema(name), name)
    }

    paths.toSet
  }

  /** Reports every key in `doc` that the schema documents, in the same path grammar
    * as [[documentedFields]]: paths restart at each named component, so a field
    * counts as observed wherever it appears.
    *
    * Where `oneOf` branches share a property name, this follows all of them, so a
    * field counts under every branch that could have produced it.
    */
  private def observedFields(
    schema: Json,
    doc: Json,
    path: String
  ): Seq[String] =
    doc.asObject match {
      case Some(obj) =>
        val alternatives = namedBranches(schema, path)

        obj.toIterable.toSeq.flatMap {
          case (key, value) =>
            alternatives.flatMap {
              case (base, alt) =>
                alt.hcursor
                  .downField("properties")
                  .downField(key)
                  .focus
                  .toSeq
                  .flatMap { sub =>
                    s"$base.$key" +: observedFields(sub, value, s"$base.$key")
                  }
            }
        }

      case None =>
        doc.asArray match {
          case Some(items) =>
            namedBranches(schema, path).flatMap {
              case (base, alt) =>
                alt.hcursor.downField("items").focus.toSeq.flatMap { s =>
                  items.flatMap(observedFields(s, _, s"$base[]"))
                }
            }

          case None => Seq.empty
        }
    }

  /** The `oneOf` alternatives of a schema, resolved, each with the path its fields
    * belong under: the component name for a `$ref`, the inline path otherwise.
    */
  private def namedBranches(
    schema: Json,
    path: String
  ): Seq[(String, Json)] = {
    def named(s: Json): (String, Json) =
      refNameOf(s) match {
        case Some(name) => (name, schemaRef(name))
        case None       => (path, s)
      }

    val (base, resolved) = named(schema)

    resolved.hcursor
      .downField("oneOf")
      .focus
      .flatMap(_.asArray)
      .map(_.map(named))
      .getOrElse(Vector((base, resolved)))
  }

  private lazy val specJson: Json = OpenApiSpec.parsed

  private def circeOf(file: File): Json = {
    val source = Source.fromFile(file)
    try io.circe.parser.parse(source.mkString).fold(throw _, identity)
    finally source.close()
  }

  private def schemaNameOf(display: Json): Option[String] =
    display.hcursor.get[String]("type").toOption.flatMap {
      case "Image"                                      => Some("Image")
      case "Work" | "Collection" | "Series" | "Section" => Some("Work")
      case _                                            => None
    }

  private def schemaRef(name: String): Json =
    specJson.hcursor
      .downField("components")
      .downField("schemas")
      .downField(name)
      .focus
      .getOrElse(throw new RuntimeException(s"No schema named $name"))

  /** Follows a `$ref` to the schema it names. */
  private def resolve(schema: Json): Json =
    schema.hcursor.get[String]("$ref").toOption match {
      case Some(ref) => schemaRef(ref.stripPrefix("#/components/schemas/"))
      case None      => schema
    }

  private def branches(schema: Json): Vector[Json] = {
    val s = resolve(schema)
    val oneOf = s.hcursor
      .downField("oneOf")
      .focus
      .flatMap(_.asArray)
      .getOrElse(Vector.empty)

    if (oneOf.isEmpty) Vector(s) else oneOf.map(resolve)
  }

  /** Reports the dotted path of any key in `doc` with no property in `schema`.
    *
    * A schema with neither `properties` nor `oneOf` is treated as opaque, so free-form
    * objects don't report every key they hold.
    */
  private def undocumentedFields(
    schema: Json,
    doc: Json,
    path: String
  ): Seq[String] =
    doc.asObject match {
      case Some(obj) =>
        val alternatives = branches(schema)
        val known = alternatives
          .flatMap(
            _.hcursor.downField("properties").keys.getOrElse(Iterable.empty)
          )
          .toSet

        if (known.isEmpty) Seq.empty
        else
          obj.toIterable.toSeq.flatMap {
            case (key, value) if !known.contains(key) => Seq(s"$path.$key")
            case (key, value) =>
              val property = alternatives
                .flatMap(
                  _.hcursor.downField("properties").downField(key).focus
                )
                .headOption

              property.toSeq.flatMap(
                undocumentedFields(_, value, s"$path.$key"))
          }

      case None =>
        doc.asArray match {
          case Some(items) =>
            val itemSchema =
              resolve(schema).hcursor.downField("items").focus

            itemSchema.toSeq.flatMap { s =>
              items.flatMap(undocumentedFields(s, _, s"$path[]"))
            }

          case None => Seq.empty
        }
    }
}
