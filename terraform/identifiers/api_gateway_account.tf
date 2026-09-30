# API Gateway writes access logs as the service itself, not as this API, so it
# needs one role set on the account for the region. That setting applies to
# every REST API in the catalogue account. No other stage in the account logs
# yet, so it lives here until another one needs it.
resource "aws_iam_role" "api_gateway_cloudwatch" {
  name               = "api-gateway-cloudwatch-logs"
  assume_role_policy = data.aws_iam_policy_document.api_gateway_assume_role.json
}

data "aws_iam_policy_document" "api_gateway_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["apigateway.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy_attachment" "api_gateway_cloudwatch" {
  role       = aws_iam_role.api_gateway_cloudwatch.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonAPIGatewayPushToCloudWatchLogs"
}

resource "aws_api_gateway_account" "main" {
  cloudwatch_role_arn = aws_iam_role.api_gateway_cloudwatch.arn

  # The role cannot be used until it has its permissions.
  depends_on = [aws_iam_role_policy_attachment.api_gateway_cloudwatch]
}
