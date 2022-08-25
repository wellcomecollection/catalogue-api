package weco.catalogue.source_model.sierra.rules

sealed trait RulesForRequestingResult

case object Requestable extends RulesForRequestingResult
sealed trait NotRequestable extends RulesForRequestingResult

object NotRequestable {
  case class InUseByAnotherReader(message: String) extends NotRequestable
}
