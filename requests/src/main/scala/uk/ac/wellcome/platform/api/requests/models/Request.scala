package uk.ac.wellcome.platform.api.requests.models

import java.time.Instant

case class RequestItem(
  id: String,
  `type`: String
)

case class Request(
  item: RequestItem,
  pickupDate: Option[Instant],
  `type`: String
)
