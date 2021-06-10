package weco.api.stacks.models

case class SierraErrorCode(
  code: Int,
  specificCode: Int,
  httpStatus: Int,
  name: String,
  description: Option[String] = None
)
