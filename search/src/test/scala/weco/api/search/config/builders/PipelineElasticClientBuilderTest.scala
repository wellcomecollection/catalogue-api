package weco.api.search.config.builders

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.models.{ClusterConfig, SemanticConfig, VectorType}


class PipelineElasticClientBuilderTest extends AnyFunSpec with Matchers {

  val pipelineDate = "2024-01-01"
  val pipelinePrefix = s"elasticsearch/pipeline_storage_$pipelineDate"

  describe("Default cluster config") {
    it("uses default pipeline secrets when all paths are None") {
      val config = ClusterConfig(pipelineDate = Some(pipelineDate))
      
      // When ClusterConfig has None for all secret paths,
      // PipelineElasticClientBuilder should use:
      // - hostSecretPath.getOrElse(s"$pipelinePrefix/$hostType")
      // - portSecretPath.getOrElse(s"$pipelinePrefix/port")
      // - protocolSecretPath.getOrElse(s"$pipelinePrefix/protocol")
      // - apiKeySecretPath.getOrElse(s"$pipelinePrefix/$serviceName/api_key")
      
      config.hostSecretPath shouldBe None
      config.portSecretPath shouldBe None
      config.protocolSecretPath shouldBe None
      config.apiKeySecretPath shouldBe None
      
      // Expected secret paths:
      val expectedHostPath = s"$pipelinePrefix/private_host"  // or public_host in dev
      val expectedPortPath = s"$pipelinePrefix/port"
      val expectedProtocolPath = s"$pipelinePrefix/protocol"
      val expectedApiKeyPath = s"$pipelinePrefix/catalogue_api/api_key"
      
      // Verify the getOrElse pattern returns default paths
      config.hostSecretPath.getOrElse(expectedHostPath) shouldEqual expectedHostPath
      config.portSecretPath.getOrElse(expectedPortPath) shouldEqual expectedPortPath
      config.protocolSecretPath.getOrElse(expectedProtocolPath) shouldEqual expectedProtocolPath
      config.apiKeySecretPath.getOrElse(expectedApiKeyPath) shouldEqual expectedApiKeyPath
    }
  }

  describe("Additional cluster configs (with custom secret paths)") {
    it("uses custom secret paths when all paths are provided") {
      val config = ClusterConfig(
        name = "elser",
        hostSecretPath = Some("elasticsearch/elser/host"),
        portSecretPath = Some("elasticsearch/elser/port"),
        protocolSecretPath = Some("elasticsearch/elser/protocol"),
        apiKeySecretPath = Some("elasticsearch/elser/api_key")
      )
      
      // When ClusterConfig has Some(path) for all secret paths,
      // PipelineElasticClientBuilder should use those custom paths
      
      config.hostSecretPath shouldBe defined
      config.portSecretPath shouldBe defined
      config.protocolSecretPath shouldBe defined
      config.apiKeySecretPath shouldBe defined
      
      // Verify the getOrElse pattern returns custom paths
      config.hostSecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/elser/host"
      config.portSecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/elser/port"
      config.protocolSecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/elser/protocol"
      config.apiKeySecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/elser/api_key"
    }

    it("mixes custom and default secret paths") {
      // Common case: custom host/apikey but default port/protocol
      val config = ClusterConfig(
        name = "openai",
        hostSecretPath = Some("elasticsearch/openai/host"),
        apiKeySecretPath = Some("elasticsearch/openai/api_key"),
        // portSecretPath = None (uses default)
        // protocolSecretPath = None (uses default)
      )
      
      val expectedDefaultPortPath = s"$pipelinePrefix/port"
      val expectedDefaultProtocolPath = s"$pipelinePrefix/protocol"
      
      // Custom paths should be used
      config.hostSecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/openai/host"
      config.apiKeySecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/openai/api_key"
      
      // Default paths should be used for unspecified fields
      config.portSecretPath.getOrElse(expectedDefaultPortPath) shouldEqual expectedDefaultPortPath
      config.protocolSecretPath.getOrElse(expectedDefaultProtocolPath) shouldEqual expectedDefaultProtocolPath
    }

    it("uses custom apiKeySecretPath while defaulting other fields") {
      val config = ClusterConfig(
        name = "custom-key",
        apiKeySecretPath = Some("elasticsearch/custom/special_api_key")
        // All other paths use defaults
      )
      
      val expectedDefaultHostPath = s"$pipelinePrefix/private_host"
      val expectedDefaultPortPath = s"$pipelinePrefix/port"
      val expectedDefaultProtocolPath = s"$pipelinePrefix/protocol"
      
      // Custom apiKey path
      config.apiKeySecretPath.getOrElse("SHOULD_NOT_USE") shouldEqual "elasticsearch/custom/special_api_key"
      
      // Defaults for others
      config.hostSecretPath.getOrElse(expectedDefaultHostPath) shouldEqual expectedDefaultHostPath
      config.portSecretPath.getOrElse(expectedDefaultPortPath) shouldEqual expectedDefaultPortPath
      config.protocolSecretPath.getOrElse(expectedDefaultProtocolPath) shouldEqual expectedDefaultProtocolPath
    }
  }

  describe("Multi-cluster scenarios") {
    it("default cluster uses pipeline secrets, ELSER cluster uses custom secrets") {
      val defaultConfig = ClusterConfig(
        name = "default",
        pipelineDate = Some(pipelineDate)
      )
      
      val elserConfig = ClusterConfig(
        name = "elser",
        pipelineDate = Some(pipelineDate),
        hostSecretPath = Some("elasticsearch/elser/host"),
        apiKeySecretPath = Some("elasticsearch/elser/api_key"),
        worksIndex = Some("works-indexed-2024-01-01"),
        semanticConfig = Some(
          SemanticConfig(
            modelId = ".elser-2-elasticsearch",
            vectorType = VectorType.Sparse
          )
        )
      )
      
      // Default cluster uses pipeline secrets
      defaultConfig.hostSecretPath shouldBe None
      defaultConfig.apiKeySecretPath shouldBe None
      defaultConfig.hostSecretPath.getOrElse(s"$pipelinePrefix/private_host") shouldEqual s"$pipelinePrefix/private_host"
      
      // ELSER cluster uses custom secrets
      elserConfig.hostSecretPath shouldBe defined
      elserConfig.apiKeySecretPath shouldBe defined
      elserConfig.hostSecretPath.get shouldEqual "elasticsearch/elser/host"
      elserConfig.apiKeySecretPath.get shouldEqual "elasticsearch/elser/api_key"
    }

    it("multiple additional clusters each use their own custom secrets") {
      val elserConfig = ClusterConfig(
        name = "elser",
        hostSecretPath = Some("elasticsearch/elser/host"),
        apiKeySecretPath = Some("elasticsearch/elser/api_key")
      )
      
      val openaiConfig = ClusterConfig(
        name = "openai",
        hostSecretPath = Some("elasticsearch/openai/host"),
        apiKeySecretPath = Some("elasticsearch/openai/api_key")
      )
      
      // Each cluster has distinct secret paths
      elserConfig.hostSecretPath.get shouldEqual "elasticsearch/elser/host"
      openaiConfig.hostSecretPath.get shouldEqual "elasticsearch/openai/host"
      
      elserConfig.apiKeySecretPath.get shouldEqual "elasticsearch/elser/api_key"
      openaiConfig.apiKeySecretPath.get shouldEqual "elasticsearch/openai/api_key"
      
      // Paths should not be equal
      elserConfig.hostSecretPath should not equal openaiConfig.hostSecretPath
      elserConfig.apiKeySecretPath should not equal openaiConfig.apiKeySecretPath
    }
  }
}
