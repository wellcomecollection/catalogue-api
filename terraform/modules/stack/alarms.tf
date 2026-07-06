# During the July 2026 search outage the API Gateway 5xx rate exceeded 90%
# and nobody was alerted: the monitoring stack's Slack lambda had been
# listening on the catalogue_api_gateway_5xx_alarm topic since it was
# created, but no alarm had ever been defined to publish to it. These are
# those alarms. The 5xx alarm follows the identity account's convention
# (any 5xx in a minute alerts); the latency alarm is a slower-burn signal
# that would have fired within a few minutes of the outage starting.

resource "aws_cloudwatch_metric_alarm" "api_gateway_5xx" {
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

  # This goes to the Chatbot topic rather than the 5xx Slack lambda,
  # because that lambda's message template describes every alarm as
  # "{n} errors in the API", which would be misleading for latency.
  alarm_actions = [var.chatbot_topic_arn]
}
