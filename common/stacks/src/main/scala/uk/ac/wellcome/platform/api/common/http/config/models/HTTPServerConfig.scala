package uk.ac.wellcome.platform.api.common.http.config.models

case class HTTPServerConfig(
  host: String,
  port: Int,
  externalBaseURL: String
)
