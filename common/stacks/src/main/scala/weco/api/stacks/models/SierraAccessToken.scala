package weco.api.stacks.models

import io.circe.generic.extras.JsonKey

// This represents a response from the Client Credentials flow, as described
// in the Sierra docs:
// https://techdocs.iii.com/sierraapi/Content/zReference/authClient.htm
case class SierraAccessToken(
  @JsonKey("access_token") accessToken: String,
  @JsonKey("expires_in") expiresIn: Int
)
