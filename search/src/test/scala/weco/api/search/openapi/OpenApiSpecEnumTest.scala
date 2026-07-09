package weco.api.search.openapi

import io.circe.Json
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.rest.{
  MultipleImagesParams,
  MultipleWorksParams,
  PaginationLimits,
  SingleImageParams,
  SingleWorkParams
}
import weco.catalogue.display_model.locations.CatalogueAccessStatus

/** Checks that the enums in reference/catalogue.yaml still describe what the API
  * actually accepts.
  *
  * The published spec drifted from the code for years — it documented an `include`
  * value that 400s, and an image colour filter under a name the API ignores — because
  * nothing tied the two together. This test is that tie.
  *
  * Scoped to works and images. The concepts service is TypeScript, so its values are
  * unreachable from here; on the request side it has no closed enums anyway (`query`,
  * `identifiers.identifierType` and `id` are all free-form strings), so there is
  * nothing there for this test to pin.
  */
class OpenApiSpecEnumTest extends AnyFunSpec with Matchers {

  private val spec: Json = OpenApiSpec.parsed

  /** Reads the enum from a named entry in `components/parameters`.
    *
    * Comma-separated parameters are modelled as arrays, so the values hang off
    * `schema/items/enum`; scalar parameters keep theirs on `schema/enum`.
    */
  private def specEnum(parameterName: String): Set[String] = {
    val schema = spec.hcursor
      .downField("components")
      .downField("parameters")
      .downField(parameterName)
      .downField("schema")

    val enum = schema
      .downField("items")
      .downField("enum")
      .focus
      .orElse(schema.downField("enum").focus)
      .getOrElse(
        fail(s"components/parameters/$parameterName has no enum in the spec")
      )

    enum.asArray
      .getOrElse(fail(s"The enum for $parameterName is not an array"))
      .map(
        _.asString.getOrElse(fail(s"Non-string enum value for $parameterName")))
      .toSet
  }

  private def checkEnum(parameterName: String, accepted: Set[String]): Unit = {
    val documented = specEnum(parameterName)

    withClue(
      s"""|components/parameters/$parameterName does not match the values the API accepts.
          |  Documented but not accepted: ${(documented -- accepted).toSeq.sorted
           .mkString(", ")}
          |  Accepted but not documented: ${(accepted -- documented).toSeq.sorted
           .mkString(", ")}
          |""".stripMargin
    ) {
      documented shouldBe accepted
    }
  }

  private def names[T](values: Seq[(String, T)]): Set[String] =
    values.map(_._1).toSet

  describe("the works parameters") {
    it("documents every include the API accepts") {
      checkEnum("WorksInclude", names(SingleWorkParams.includeValues))
    }

    it("documents every aggregation the API accepts") {
      checkEnum(
        "WorksAggregations",
        names(MultipleWorksParams.aggregationValues))
    }

    it("documents every sort the API accepts") {
      checkEnum("WorksSort", names(MultipleWorksParams.sortValues))
    }

    it("documents every access status the filter accepts") {
      checkEnum("WorksAccessStatusFilter", CatalogueAccessStatus.values)
    }
  }

  describe("the images parameters") {
    it("documents every include the /images endpoint accepts") {
      checkEnum("ImagesInclude", names(MultipleImagesParams.includeValues))
    }

    it("documents every include the /images/{id} endpoint accepts") {
      checkEnum("ImagesIncludeSingle", names(SingleImageParams.includeValues))
    }

    it("documents every aggregation the API accepts") {
      checkEnum(
        "ImagesAggregations",
        names(MultipleImagesParams.aggregationValues))
    }

    it("documents every sort the API accepts") {
      checkEnum("ImagesSort", names(MultipleImagesParams.sortValues))
    }
  }

  describe("the shared parameters") {
    it("documents the sort order the API accepts") {
      // Both endpoints accept the same values, so one spec parameter serves both.
      names(MultipleWorksParams.sortOrderValues) shouldBe names(
        MultipleImagesParams.sortOrderValues
      )
      checkEnum("SortOrder", names(MultipleWorksParams.sortOrderValues))
    }

    it("documents the page size limits the API enforces") {
      val schema = spec.hcursor
        .downField("components")
        .downField("parameters")
        .downField("PageSize")
        .downField("schema")

      schema.get[Int]("minimum").toOption shouldBe Some(
        PaginationLimits.minSize)
      schema.get[Int]("maximum").toOption shouldBe Some(
        PaginationLimits.maxSize)
    }
  }
}
