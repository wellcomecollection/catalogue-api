package weco.api.search.config

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MultiClusterConfigParserTest extends AnyFunSpec with Matchers {

  describe("parseMultiClusterConfig") {
    it("returns empty map when no multiCluster config exists") {
      val config = ConfigFactory.parseString("""
        |someOtherConfig = "value"
        |""".stripMargin)

      val result = MultiClusterConfigParser.parse(config)

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

      val result = MultiClusterConfigParser.parse(config)

      result should have size 1
      result should contain key "elser"
      
      val elserConfig = result("elser")
      elserConfig.name shouldBe "elser"
      elserConfig.hostSecretPath shouldBe Some("custom/host")
      elserConfig.apiKeySecretPath shouldBe Some("custom/apikey")
      elserConfig.worksIndex shouldBe Some("works-elser-full")
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

      val result = MultiClusterConfigParser.parse(config)

      result should have size 2
      result should contain key "elser"
      result should contain key "openai"
      
      result("elser").worksIndex shouldBe Some("works-elser-full")
      result("openai").worksIndex shouldBe Some("works-openai-full")
    }

    it("returns None for semanticConfig when vectorType is invalid") {
      val config = ConfigFactory.parseString("""
        |multiCluster.test {
        |  hostSecretPath = "test/host"
        |  apiKeySecretPath = "test/apikey"
        |  worksIndex = "works-test"
        |  semanticModelId = "some-model"
        |  semanticVectorType = "invalid"
        |}
        |""".stripMargin)

      val result = MultiClusterConfigParser.parse(config)

      result("test").semanticConfig shouldBe None
    }

    it("returns None for semanticConfig when modelId is missing") {
      val config = ConfigFactory.parseString("""
        |multiCluster.test {
        |  hostSecretPath = "test/host"
        |  apiKeySecretPath = "test/apikey"
        |  worksIndex = "works-test"
        |  semanticVectorType = "dense"
        |}
        |""".stripMargin)

      val result = MultiClusterConfigParser.parse(config)

      result("test").semanticConfig shouldBe None
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

      val result = MultiClusterConfigParser.parse(config)

      result("test").worksIndex shouldBe Some("works-test")
      result("test").imagesIndex shouldBe Some("images-test")
    }

    it("handles missing optional fields") {
      val config = ConfigFactory.parseString("""
        |multiCluster.minimal {
        |  hostSecretPath = "minimal/host"
        |  apiKeySecretPath = "minimal/apikey"
        |}
        |""".stripMargin)

      val result = MultiClusterConfigParser.parse(config)

      val minimalConfig = result("minimal")
      minimalConfig.worksIndex shouldBe None
      minimalConfig.imagesIndex shouldBe None
      minimalConfig.semanticConfig shouldBe None
    }
  }
}
