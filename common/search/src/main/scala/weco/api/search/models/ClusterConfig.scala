package weco.api.search.models
import com.sksamuel.elastic4s.Index

/** Configuration for a single Elasticsearch cluster */
case class ClusterConfig(
  name: String,
  worksIndex: Index,
  customHost: String,
  customApiKeySecretPath: String,
  customPort: Option[Int] = None,
  customProtocol: Option[String] = None,
  semanticModelId: Option[String] = None,
  semanticVectorType: Option[String] = None
)
