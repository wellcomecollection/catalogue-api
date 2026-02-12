package weco.api.search.config

import com.sksamuel.elastic4s.Index
import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models.VectorType

class MultiElasticConfigParserTest extends AnyFunSpec with Matchers {

  describe("parseMultiElasticConfig") {
    it("returns empty map when no multiCluster config exists") {
      val config = ConfigFactory.parseString("""
        |someOtherConfig = "value"
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result shouldBe empty
    }

    it("parses a single cluster configuration") {
      val config = ConfigFactory.parseString("""
        |multiCluster.elser {
        |  hostSecretPath = "custom/host"
        |  apiKeySecretPath = "custom/apikey"
        |  worksIndex = "works-elser-full"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result should have size 1
      result should contain key "elser"
      
      val elserConfig = result("elser")
      elserConfig.name shouldBe "elser"
      elserConfig.hostSecretPath shouldBe Some("custom/host")
      elserConfig.apiKeySecretPath shouldBe Some("custom/apikey")
      elserConfig.worksIndex shouldBe Some(Index("works-elser-full"))
      elserConfig.semanticConfig shouldBe None
    }

    it("parses multiple cluster configurations") {
      val config = ConfigFactory.parseString("""
        |multiCluster.elser {
        |  hostSecretPath = "elser/host"
        |  apiKeySecretPath = "elser/apikey"
        |  worksIndex = "works-elser-full"
        |}
        |multiCluster.openai {
        |  hostSecretPath = "openai/host"
        |  apiKeySecretPath = "openai/apikey"
        |  worksIndex = "works-openai-full"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result should have size 2
      result should contain key "elser"
      result should contain key "openai"
      
      result("elser").worksIndex shouldBe Some(Index("works-elser-full"))
      result("openai").worksIndex shouldBe Some(Index("works-openai-full"))
    }

    it("excludes config when semantic vectorType is invalid") {
      val config = ConfigFactory.parseString("""
        |multiCluster.test {
        |  hostSecretPath = "test/host"
        |  apiKeySecretPath = "test/apikey"
        |  worksIndex = "works-test"
        |  semantic {
        |    modelId = "some-model"
        |    vectorType = "invalid"
        |  }
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)
      result shouldBe empty
    }

    it("excludes config when semantic modelId is missing") {
      val config = ConfigFactory.parseString("""
        |multiCluster.test {
        |  hostSecretPath = "test/host"
        |  apiKeySecretPath = "test/apikey"
        |  worksIndex = "works-test"
        |  semantic {
        |    vectorType = "dense"
        |  }
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result shouldBe empty
    }

    it("parses optional images index") {
      val config = ConfigFactory.parseString("""
        |multiCluster.test {
        |  hostSecretPath = "test/host"
        |  apiKeySecretPath = "test/apikey"
        |  worksIndex = "works-test"
        |  imagesIndex = "images-test"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result("test").worksIndex shouldBe Some(Index("works-test"))
      result("test").imagesIndex shouldBe Some(Index("images-test"))
    }

    it("parses semantic config tuning parameters") {
      val config = ConfigFactory.parseString("""
        |multiCluster.test {
        |  hostSecretPath = "test/host"
        |  apiKeySecretPath = "test/apikey"
        |  worksIndex = "works-test"
        |  semantic {
        |    modelId = "some-model"
        |    vectorType = "dense"
        |    k = 10
        |    numCandidates = 200
        |    rankWindowSize = 5000
        |    rankConstant = 30
        |  }
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      val semanticConfig = result("test").semanticConfig
      semanticConfig.isDefined shouldBe true
      val semantic = semanticConfig.get
      semantic.modelId shouldBe "some-model"
      semantic.vectorType shouldBe VectorType.Dense
      semantic.k shouldBe 10
      semantic.numCandidates shouldBe 200
      semantic.rankWindowSize shouldBe 5000
      semantic.rankConstant shouldBe 30
    }

    it("handles missing optional fields") {
      val config = ConfigFactory.parseString("""
        |multiCluster.minimal {
        |  hostSecretPath = "minimal/host"
        |  apiKeySecretPath = "minimal/apikey"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      val minimalConfig = result("minimal")
      minimalConfig.worksIndex shouldBe None
      minimalConfig.imagesIndex shouldBe None
      minimalConfig.semanticConfig shouldBe None
    }

    it("excludes config when mandatory fields are missing") {
      val config = ConfigFactory.parseString("""
        |multiCluster.invalid {
        |  hostSecretPath = "invalid/host"
        |  worksIndex = "works-invalid"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result shouldBe empty
    }
  }
}
