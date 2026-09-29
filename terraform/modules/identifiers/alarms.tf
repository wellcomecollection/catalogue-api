resource "aws_cloudwatch_metric_alarm" "api_gateway_5xx" {
  count = var.enable_api_alarms ? 1 : 0

  alarm_name          = "identifiers-api-${var.environment_name}-5xx-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "5XXError"
  namespace           = "AWS/ApiGateway"
  period              = 60
  statistic           = "Sum"
  # Unlike the catalogue API there is no background of 5xxs, so any is worth
  # looking at.
  threshold          = 0
  treat_missing_data = "notBreaching"

  dimensions = {
    ApiName = aws_api_gateway_rest_api.identifiers.name
  }

  alarm_actions = [var.api_gateway_alerts_topic_arn]
}

locals {
  # A placeholder well under the Lambda's 10s timeout, until the load test in
  # wellcomecollection/platform#6536 gives a real number.
  latency_alarm_threshold_ms = 3000
  latency_alarm_minutes      = 3
}

resource "aws_cloudwatch_metric_alarm" "api_gateway_latency" {
  count = var.enable_api_alarms ? 1 : 0

  alarm_name          = "identifiers-api-${var.environment_name}-latency-alarm"
  alarm_description   = "p99 latency for ${aws_api_gateway_rest_api.identifiers.name} has been over ${local.latency_alarm_threshold_ms}ms for ${local.latency_alarm_minutes} minutes"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = local.latency_alarm_minutes
  metric_name         = "Latency"
  namespace           = "AWS/ApiGateway"
  period              = 60
  extended_statistic  = "p99"
  threshold           = local.latency_alarm_threshold_ms
  treat_missing_data  = "notBreaching"

  dimensions = {
    ApiName = aws_api_gateway_rest_api.identifiers.name
  }

  # Chatbot rather than the 5xx Slack lambda, whose message template
  # only describes error counts.
  alarm_actions = [var.chatbot_topic_arn]
}

# API Gateway publishes no metric for requests it throttles, so count the 429s
# in the access logs instead. Every value in the access log format is a JSON
# string, so the status has to be matched as one.
resource "aws_cloudwatch_log_metric_filter" "throttled_requests" {
  name           = "identifiers-api-${var.environment_name}-throttled-requests"
  log_group_name = aws_cloudwatch_log_group.access_logs.name
  pattern        = "{ $.status = \"429\" }"

  metric_transformation {
    name      = "ThrottledRequests-${var.environment_name}"
    namespace = "IdentifiersApi"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "throttled_requests" {
  count = var.enable_api_alarms ? 1 : 0

  alarm_name          = "identifiers-api-${var.environment_name}-throttled-requests-alarm"
  alarm_description   = "${aws_api_gateway_rest_api.identifiers.name} is rejecting requests with 429 because a consumer is over its usage plan or the account-level limit"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = aws_cloudwatch_log_metric_filter.throttled_requests.metric_transformation[0].name
  namespace           = aws_cloudwatch_log_metric_filter.throttled_requests.metric_transformation[0].namespace
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  alarm_actions = [var.chatbot_topic_arn]
}
