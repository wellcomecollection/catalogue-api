resource "aws_cloudwatch_metric_alarm" "api_gateway_5xx" {
  count = var.enable_api_alarms ? 1 : 0

  alarm_name          = "catalogue-api-${var.environment_name}-5xx-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "5XXError"
  namespace           = "AWS/ApiGateway"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  treat_missing_data  = "notBreaching"

  dimensions = {
    ApiName = aws_api_gateway_rest_api.catalogue.name
  }

  alarm_actions = [var.api_gateway_alerts_topic_arn]
}

resource "aws_cloudwatch_metric_alarm" "api_gateway_latency" {
  count = var.enable_api_alarms ? 1 : 0

  alarm_name          = "catalogue-api-${var.environment_name}-latency-alarm"
  alarm_description   = "p95 latency for ${aws_api_gateway_rest_api.catalogue.name} has been over 10s for 3 minutes"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "Latency"
  namespace           = "AWS/ApiGateway"
  period              = 60
  extended_statistic  = "p95"
  threshold           = 10000
  treat_missing_data  = "notBreaching"

  dimensions = {
    ApiName = aws_api_gateway_rest_api.catalogue.name
  }

  # Chatbot rather than the 5xx Slack lambda, whose message template
  # only describes error counts.
  alarm_actions = [var.chatbot_topic_arn]
}
