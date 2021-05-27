resource "aws_cloudwatch_event_rule" "snapshot_scheduler_rule" {
  name        = "snapshot_scheduler_rule-${var.deployment_service_env}"
  description = "Starts the snapshot_scheduler (${var.deployment_service_env}) lambda"

  # At 11pm every day
  schedule_expression = "cron(23 0 * * ? *)"
}
