package weco.api.search.models
import com.sksamuel.elastic4s.Index

/** Configuration for a single Elasticsearch cluster */
case class ClusterConfig(
  name: String = "default",
  worksIndex: Option[Index] = None,
  hostSecretPath: Option[String] = None,
  apiKeySecretPath: Option[String] = None,
  portSecretPath: Option[String] = None,
  protocolSecretPath: Option[String] = None,
  semanticModelId: Option[String] = None,
  semanticVectorType: Option[String] = None
)
