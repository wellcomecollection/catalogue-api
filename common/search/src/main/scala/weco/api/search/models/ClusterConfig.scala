package weco.api.search.models
import com.sksamuel.elastic4s.Index

/** Configuration for a single Elasticsearch cluster */
case class ClusterConfig(
  name: String,
  worksIndex: Index,
  customHost: String,
  semanticModelId: Option[String] = None,
  customPort: Option[Int] = None,
  customProtocol: Option[String] = None,
  customApiKeySecretPath: String,
  semanticVectorType: Option[String] = None
)
