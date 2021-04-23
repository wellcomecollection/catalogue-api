module "snapshot_scheduler" {
  source = "../../modules/lambda"

  name = "snapshot_scheduler-${var.deployment_service_env}"

  s3_bucket = var.lambda_upload_bucket
  s3_key    = "lambdas/snapshots/snapshot_scheduler.zip"

  description     = "Send snapshot schedules to an SNS topic"
  alarm_topic_arn = var.lambda_error_alarm_arn
  timeout         = 10

  env_vars = {
    TOPIC_ARN            = module.scheduler_topic.arn
    PUBLIC_BUCKET_NAME   = var.public_bucket_name
    PUBLIC_OBJECT_KEY_V2 = var.public_object_key_v2
  }

  log_retention_in_days = 30

  handler = "snapshot_scheduler"
}

resource "aws_lambda_permission" "allow_cloudwatch_trigger" {
  action        = "lambda:InvokeFunction"
  function_name = module.snapshot_scheduler.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.snapshot_scheduler_rule.arn
}

resource "aws_cloudwatch_event_target" "event_trigger" {
  rule = aws_cloudwatch_event_rule.snapshot_scheduler_rule.id
  arn  = module.snapshot_scheduler.arn
}
