package weco.api.search.config

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

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
        |multiCluster.cluster-a {
        |  hostSecretPath = "custom/host"
        |  apiKeySecretPath = "custom/apikey"
        |  worksIndex = "works-cluster-a-full"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result should have size 1
      result should contain key "cluster-a"
      
      val clusterAConfig = result("cluster-a")
      clusterAConfig.name shouldBe "cluster-a"
      clusterAConfig.hostSecretPath shouldBe Some("custom/host")
      clusterAConfig.apiKeySecretPath shouldBe Some("custom/apikey")
      clusterAConfig.worksIndex shouldBe Some("works-cluster-a-full")
    }

    it("parses multiple cluster configurations") {
      val config = ConfigFactory.parseString("""
        |multiCluster.cluster-a {
        |  hostSecretPath = "cluster-a/host"
        |  apiKeySecretPath = "cluster-a/apikey"
        |  worksIndex = "works-cluster-a-full"
        |}
        |multiCluster.cluster-b {
        |  hostSecretPath = "cluster-b/host"
        |  apiKeySecretPath = "cluster-b/apikey"
        |  worksIndex = "works-cluster-b-full"
        |}
        |""".stripMargin)

      val result = MultiElasticConfigParser.parse(config)

      result should have size 2
      result should contain key "cluster-a"
      result should contain key "cluster-b"
      
      result("cluster-a").worksIndex shouldBe Some("works-cluster-a-full")
      result("cluster-b").worksIndex shouldBe Some("works-cluster-b-full")
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

      val result = MultiElasticConfigParser.parse(config)

      val minimalConfig = result("minimal")
      minimalConfig.worksIndex shouldBe None
      minimalConfig.imagesIndex shouldBe None
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

    // A bad entry is dropped at startup, so check the preview parses from the real config.
    it("parses the pipeline-2026-09-30 preview from application.conf") {
      val config = ConfigFactory.parseResources("application.conf").resolve()

      val preview = MultiElasticConfigParser.parse(config)("pipeline-2026-09-30")

      preview.hostSecretPath shouldBe Some("elasticsearch/pipeline_storage_2026-09-30/private_host")
      preview.portSecretPath shouldBe Some("elasticsearch/pipeline_storage_2026-09-30/port")
      preview.protocolSecretPath shouldBe Some("elasticsearch/pipeline_storage_2026-09-30/protocol")
      preview.apiKeySecretPath shouldBe Some("elasticsearch/pipeline_storage_2026-09-30/catalogue_api/api_key")
      preview.worksIndex shouldBe Some("works-indexed-2026-09-30")
      preview.imagesIndex shouldBe Some("images-indexed-2026-09-30")
    }
  }
}
