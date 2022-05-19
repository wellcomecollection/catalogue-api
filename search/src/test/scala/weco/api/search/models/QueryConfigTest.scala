package weco.api.search.models

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.fixtures.TestDocumentFixtures
import weco.catalogue.internal_model.index.IndexFixtures
import weco.elasticsearch.ElasticClientBuilder

import scala.concurrent.ExecutionContext.Implicits.global

class QueryConfigTest
    extends AnyFunSpec
    with Matchers
    with IndexFixtures
    with TestDocumentFixtures {
  describe("fetchFromIndex") {
    it("fetches query config from a given index") {
      withLocalImagesIndex { index =>
        indexTestDocuments(index, "images.similar-features-and-palettes.0")

        val result = QueryConfig.fetchFromIndex(elasticClient, index)
        result.paletteBinSizes shouldBe List(
          List(1, 7, 5),
          List(0, 5, 7),
          List(6, 5, 1)
        )

        // Casting to string here is to avoid weirdness when comparing Doubles;
        // if you compare to List(0.34999806, 0.7922977, 0.3721038), Scala will
        // tell you they're different.
        result.paletteBinMinima.map(_.toString) shouldBe List(
          "0.7710878",
          "0.8503088",
          "0.6996027"
        )
      }
    }

    it("returns the default config if the index doesn't exist") {
      val result =
        QueryConfig.fetchFromIndex(elasticClient, Index("not-an-index"))
      result.paletteBinSizes shouldBe QueryConfig.defaultPaletteBinSizes
    }

    it("returns the default config if ES can't be connected to") {
      val badClient = ElasticClientBuilder.create(
        hostname = "not-a-host",
        port = 123456,
        protocol = "http",
        username = "not-good",
        password = "very-bad"
      )
      val result =
        QueryConfig.fetchFromIndex(badClient, Index("not-an-index"))
      result.paletteBinSizes shouldBe QueryConfig.defaultPaletteBinSizes
    }

    it("returns the default config if the data is not in the expected format") {
      withLocalImagesIndex { index =>
        indexTestDocuments(index, "images.inferred-data.wrong-format")

        val result = QueryConfig.fetchFromIndex(elasticClient, index)
        result.paletteBinSizes shouldBe QueryConfig.defaultPaletteBinSizes
      }
    }

    it("returns the default config if the data is not found") {
      withLocalImagesIndex { index =>
        indexTestDocuments(index, "images.inferred-data.none")

        val result = QueryConfig.fetchFromIndex(elasticClient, index)
        result.paletteBinSizes shouldBe QueryConfig.defaultPaletteBinSizes
      }
    }
  }
}
