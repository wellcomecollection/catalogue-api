package weco.api.search.services

import com.sksamuel.elastic4s.Index
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.models.index.IndexedImage
import weco.api.search.models.{ImageSearchOptions, RgbColor, SearchQuery}

// The feature vector is 4096 floats per image; hits must not fetch it.
class ImagesRequestBuilderTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with TableDrivenPropertyChecks {

  private val index = Index("images")

  private def sourceIncludes(searchOptions: ImageSearchOptions): Seq[String] =
    ImagesRequestBuilder
      .request(searchOptions, index)
      .value
      .params
      .hcursor
      .get[Seq[String]]("includes")
      .right
      .value

  describe("search results") {
    val searches = Table(
      ("search", "searchOptions"),
      ("listing", ImageSearchOptions()),
      ("query", ImageSearchOptions(searchQuery = Some(SearchQuery("focaccia")))),
      ("colour", ImageSearchOptions(color = Some(RgbColor(255, 0, 0))))
    )

    it("fetches only the display document") {
      forAll(searches) { (_, searchOptions) =>
        sourceIncludes(searchOptions) shouldBe Seq("display")
      }
    }
  }

  describe("similar images") {
    it("queries with the image's feature vector but fetches only display") {
      val image = IndexedImage(
        display = Json.obj(),
        vectorValues = Json.obj(
          "features" -> Json.arr(Json.fromFloatOrNull(0.5f))
        )
      )

      val request = ImagesRequestBuilder
        .requestWithSimilarFeatures(index, "abc123", image, 5, 0.0)

      val body = parse(request.source.get).right.value.hcursor

      body
        .downField("knn")
        .get[String]("field")
        .right
        .value shouldBe "vectorValues.features"
      body
        .downField("knn")
        .get[Seq[Float]]("query_vector")
        .right
        .value shouldBe Seq(0.5f)
      body
        .downField("_source")
        .get[Seq[String]]("includes")
        .right
        .value shouldBe Seq("display")
    }
  }
}
