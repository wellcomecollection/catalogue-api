package weco.api.stacks.models

// This represents a response from the Client Credentials flow, as described
// in the Sierra docs:
// https://techdocs.iii.com/sierraapi/Content/zReference/authClient.htm
//
// Note: these use snake_case instead of camelCase field names so they can be
// correctly decoded by the akka-http Unmarshaller when they get received from
// the Sierra API.  I tried using the @JsonKey annotation, couldn't get it working,
// and didn't want to spend more time debugging it.
case class SierraAccessToken(
  access_token: String,
  expires_in: Int
)
