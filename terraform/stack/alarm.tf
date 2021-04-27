//TODO does this do anything
resource "aws_cloudwatch_metric_alarm" "server_error" {
  alarm_name                = "catalogue-api-${var.environment_name}-500x"
  comparison_operator       = "GreaterThanOrEqualToThreshold"
  evaluation_periods        = "1"
  metric_name               = "5XXError"
  namespace                 = "AWS/ApiGateway"
  period                    = "60"
  statistic                 = "Sum"
  threshold                 = "10"
  alarm_description         = "This metric monitors 500s from the Catalogue API (${var.environment_name})"
  alarm_actions             = [aws_sns_topic.server_error_alarm.arn]
  insufficient_data_actions = []

  dimensions = {
    Stage   = var.environment_name
    ApiName = "Catalogue API"
  }
}

resource "aws_sns_topic" "server_error_alarm" {
  name = "catalogue-api-${var.environment_name}-500x"
}
