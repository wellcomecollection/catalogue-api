resource "aws_cloudwatch_event_rule" "snapshot_scheduler_rule" {
  name        = "snapshot_scheduler_rule-${var.deployment_service_env}"
  description = "Starts the snapshot_scheduler (${var.deployment_service_env}) lambda"
  # Between 2-3am every day
  schedule_expression = "cron(23 0 * * ? *)"
}
