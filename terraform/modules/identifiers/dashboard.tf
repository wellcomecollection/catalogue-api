resource "aws_cloudwatch_dashboard" "identifiers" {
  dashboard_name = "identifiers-api-${var.environment_name}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "log"
        x      = 0
        y      = 0
        width  = 24
        height = 6
        properties = {
          title  = "Requests by API key"
          region = "eu-west-1"
          view   = "timeSeries"
          query  = "SOURCE '${aws_cloudwatch_log_group.access_logs.name}' | stats count(*) as requests by apiKeyId, bin(5m)"
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 6
        width  = 24
        height = 6
        properties = {
          title  = "Responses by status and error type"
          region = "eu-west-1"
          view   = "table"
          query  = "SOURCE '${aws_cloudwatch_log_group.access_logs.name}' | stats count(*) as requests by status, errorType | sort requests desc"
        }
      },
    ]
  })
}
