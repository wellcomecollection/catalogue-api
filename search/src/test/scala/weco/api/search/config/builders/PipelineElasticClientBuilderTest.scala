package weco.api.search.config.builders

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import com.sksamuel.elastic4s.Index
import weco.api.search.models.{ElasticConfig, SemanticConfig, VectorType}


class PipelineElasticClientBuilderTest extends AnyFunSpec with Matchers {

  val pipelineDate = "2024-01-01"
  val pipelinePrefix = s"elasticsearch/pipeline_storage_$pipelineDate"

  describe("Default cluster config") {
    it("uses default pipeline secrets for the default cluster") {
      val config = ElasticConfig.forDefaultCluster(
        serviceName = "catalogue_api",
        pipelineDate = pipelineDate
      )

      val expectedHostPath = s"$pipelinePrefix/private_host" // or public_host in dev
      val expectedPortPath = s"$pipelinePrefix/port"
      val expectedProtocolPath = s"$pipelinePrefix/protocol"
      val expectedApiKeyPath = s"$pipelinePrefix/catalogue_api/api_key"

      config.hostSecretPath shouldEqual expectedHostPath
      config.portSecretPath shouldEqual expectedPortPath
      config.protocolSecretPath shouldEqual expectedProtocolPath
      config.apiKeySecretPath shouldEqual expectedApiKeyPath
    }
  }

  describe("Additional cluster configs (with custom secret paths)") {
    it("uses custom secret paths when all paths are provided") {
      val config = ElasticConfig(
        name = "elser",
        hostSecretPath = "elasticsearch/elser/host",
        portSecretPath = "elasticsearch/elser/port",
        protocolSecretPath = "elasticsearch/elser/protocol",
        apiKeySecretPath = "elasticsearch/elser/api_key"
      )

      config.hostSecretPath shouldEqual "elasticsearch/elser/host"
      config.portSecretPath shouldEqual "elasticsearch/elser/port"
      config.protocolSecretPath shouldEqual "elasticsearch/elser/protocol"
      config.apiKeySecretPath shouldEqual "elasticsearch/elser/api_key"
    }
  }

  describe("Multi-cluster scenarios") {
    it("default cluster uses pipeline secrets, ELSER cluster uses custom secrets") {
      val defaultConfig = ElasticConfig.forDefaultCluster(
        serviceName = "catalogue_api",
        pipelineDate = pipelineDate
      )
      
      val elserConfig = ElasticConfig(
        name = "elser",
        hostSecretPath = "elasticsearch/elser/host",
        apiKeySecretPath = "elasticsearch/elser/api_key",
        portSecretPath = "elasticsearch/elser/port",
        protocolSecretPath = "elasticsearch/elser/protocol",
        worksIndex = Some(Index("works-indexed-2024-01-01")),
        semanticConfig = Some(
          SemanticConfig(
            modelId = ".elser-2-elasticsearch",
            vectorType = VectorType.Sparse
          )
        )
      )
      
      // Default cluster uses pipeline secrets
      defaultConfig.hostSecretPath shouldEqual s"$pipelinePrefix/private_host"
      
      // ELSER cluster uses custom secrets
      elserConfig.hostSecretPath shouldEqual "elasticsearch/elser/host"
      elserConfig.apiKeySecretPath shouldEqual "elasticsearch/elser/api_key"
    }

    it("multiple additional clusters each use their own custom secrets") {
      val elserConfig = ElasticConfig(
        name = "elser",
        hostSecretPath = "elasticsearch/elser/host",
        apiKeySecretPath = "elasticsearch/elser/api_key",
        portSecretPath = "elasticsearch/elser/port",
        protocolSecretPath = "elasticsearch/elser/protocol"
      )
      
      val openaiConfig = ElasticConfig(
        name = "openai",
        hostSecretPath = "elasticsearch/openai/host",
        apiKeySecretPath = "elasticsearch/openai/api_key",
        portSecretPath = "elasticsearch/openai/port",
        protocolSecretPath = "elasticsearch/openai/protocol"
      )
      
      // Each cluster has distinct secret paths
      elserConfig.hostSecretPath shouldEqual "elasticsearch/elser/host"
      openaiConfig.hostSecretPath shouldEqual "elasticsearch/openai/host"
      
      elserConfig.apiKeySecretPath shouldEqual "elasticsearch/elser/api_key"
      openaiConfig.apiKeySecretPath shouldEqual "elasticsearch/openai/api_key"
      
      // Paths should not be equal
      elserConfig.hostSecretPath should not equal openaiConfig.hostSecretPath
      elserConfig.apiKeySecretPath should not equal openaiConfig.apiKeySecretPath
    }
  }
}
