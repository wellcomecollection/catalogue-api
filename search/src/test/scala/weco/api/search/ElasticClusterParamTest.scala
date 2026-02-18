package weco.api.search

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.models.ElasticConfig


// NOTE: we don not pass a SemanticConfig for the additionalClusters because running RFF queries requires a license 
// this test only checks that we're routed to the correct index, so we don't need to test RFF functionality here

class ElasticClusterParamTest
    extends AnyFunSpec
    with Matchers
    with ApiTestBase
    with TestDocumentFixtures {

  describe("elasticCluster query parameter") {
    it("uses default controller when no elasticCluster param is provided") {
      withMultiClusterApi(
        defaultElastic = ElasticConfig.forDefaultCluster(serviceName="catalogue_api"),
        additionalElastics = Map(
          "elser" -> ElasticConfig(
            name = "elser",
            hostSecretPath = "elser/host",
            apiKeySecretPath = "elser/key",
            portSecretPath = "elser/port",
            protocolSecretPath = "elser/protocol",
            worksIndex = Some(Index("works-elser"))
          )
        )
      ) {
        case (indices, routes) =>
          val defaultWorkId = getKey(getVisibleWork(visibleWorks(0)).display, "id").get.asString.get
          val elserWorkId = getKey(getVisibleWork(visibleWorks(3)).display, "id").get.asString.get
          
          indexTestDocuments(indices("default"), visibleWorks(0))
          indexTestDocuments(indices("elser"), visibleWorks(3))

          // Without param, should use default
          assertJsonResponseLike(routes, s"$rootPath/works") { json =>
            val jsonStr = json.toString()
            jsonStr should include(defaultWorkId)
            jsonStr should not include elserWorkId
          }
      }
    }

    it("routes to correct controller when elasticCluster param is provided") {
      withMultiClusterApi(
        defaultElastic = ElasticConfig.forDefaultCluster(serviceName="catalogue_api"),
        additionalElastics = Map(
          "elser" -> ElasticConfig(
            name = "elser",
            hostSecretPath = "elser/host",
            apiKeySecretPath = "elser/key",
            portSecretPath = "elser/port",
            protocolSecretPath = "elser/protocol",
            worksIndex = Some(Index("works-elser"))
          ),
          "openai" -> ElasticConfig(
            name = "openai",
            hostSecretPath = "openai/host",
            apiKeySecretPath = "openai/key",
            portSecretPath = "openai/port",
            protocolSecretPath = "openai/protocol",
            worksIndex = Some(Index("works-openai"))
          )
        )
      ) {
        case (indices, routes) =>
          val defaultWorkId = getKey(getVisibleWork(visibleWorks(0)).display, "id").get.asString.get
          val elserWorkId = getKey(getVisibleWork(visibleWorks(3)).display, "id").get.asString.get
          val openaiWorkId = getKey(getVisibleWork(visibleWorks(4)).display, "id").get.asString.get

          
          indexTestDocuments(indices("default"), visibleWorks(0))
          indexTestDocuments(indices("elser"), visibleWorks(3))
          indexTestDocuments(indices("openai"), visibleWorks(4))

          // With param, should use elser controller
          assertJsonResponseLike(routes, s"$rootPath/works?elasticCluster=elser") { json =>
            val jsonStr = json.toString()
            jsonStr should include(elserWorkId)
            jsonStr should not include defaultWorkId
            jsonStr should not include openaiWorkId
          }
      }
    }

    it("returns 404 when elasticCluster param references unknown cluster") {
      withMultiClusterApi(
        defaultElastic = ElasticConfig.forDefaultCluster(serviceName="catalogue_api"),
        additionalElastics = Map.empty
      ) {
        case (indices, routes) =>
          indexTestDocuments(indices("default"), works.take(3): _*)

          Get(s"$rootPath/works?elasticCluster=unknown") ~> routes ~> check {
            status shouldEqual Status.NotFound
            val json = responseAs[String]
            json should include("Cluster 'unknown' is not configured")
          }
      }
    }

    it("supports multiple clusters") {
      withMultiClusterApi(
        defaultElastic = ElasticConfig.forDefaultCluster(serviceName="catalogue_api"),
        additionalElastics = Map(
          "elser" -> ElasticConfig(
            name = "elser",
            hostSecretPath = "elser/host",
            apiKeySecretPath = "elser/key",
            portSecretPath = "elser/port",
            protocolSecretPath = "elser/protocol",
            worksIndex = Some(Index("works-elser"))
          ),
          "openai" -> ElasticConfig(
            name = "openai",
            hostSecretPath = "openai/host",
            apiKeySecretPath = "openai/key",
            portSecretPath = "openai/port",
            protocolSecretPath = "openai/protocol",
            worksIndex = Some(Index("works-openai"))
          )
        )
      ) {
        case (indices, routes) =>
          val defaultWorkId = getKey(getVisibleWork(visibleWorks(0)).display, "id").get.asString.get
          val elserWorkId = getKey(getVisibleWork(visibleWorks(3)).display, "id").get.asString.get
          val openaiWorkId = getKey(getVisibleWork(visibleWorks(4)).display, "id").get.asString.get
          
          indexTestDocuments(indices("default"), visibleWorks(0))
          indexTestDocuments(indices("elser"), visibleWorks(3))
          indexTestDocuments(indices("openai"), visibleWorks(4))
          // Default cluster
          assertJsonResponseLike(routes, s"$rootPath/works") { json =>
            val jsonStr = json.toString()
            jsonStr should include(defaultWorkId)
            jsonStr should not include elserWorkId
            jsonStr should not include openaiWorkId
          }

          // ELSER cluster
          assertJsonResponseLike(routes, s"$rootPath/works?elasticCluster=elser") { json =>
            val jsonStr = json.toString()
            jsonStr should include(elserWorkId)
            jsonStr should not include defaultWorkId
            jsonStr should not include openaiWorkId
          }

          // OpenAI cluster
          assertJsonResponseLike(routes, s"$rootPath/works?elasticCluster=openai") { json =>
            val jsonStr = json.toString()
            jsonStr should include(openaiWorkId)
            jsonStr should not include defaultWorkId
            jsonStr should not include elserWorkId
          }
      }
    }
  }
}
