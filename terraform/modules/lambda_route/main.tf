# One path part with one method, integrated with a Lambda as a proxy. The
# Lambda gets the gateway's resource path, where api_route rewrites the path
# for a service behind the load balancer.
resource "aws_api_gateway_resource" "resource" {
  rest_api_id = var.rest_api_id
  parent_id   = var.parent_id
  path_part   = var.path_part
}

resource "aws_api_gateway_method" "method" {
  rest_api_id = var.rest_api_id
  resource_id = aws_api_gateway_resource.resource.id
  http_method = var.http_method

  authorization    = "NONE"
  api_key_required = var.api_key_required
}

resource "aws_api_gateway_integration" "integration" {
  rest_api_id = var.rest_api_id
  resource_id = aws_api_gateway_resource.resource.id
  http_method = aws_api_gateway_method.method.http_method

  # Lambda is always invoked with a POST, whatever the method's verb.
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.lambda_invoke_arn
}

resource "aws_lambda_permission" "invoke" {
  action        = "lambda:InvokeFunction"
  function_name = var.lambda_name
  principal     = "apigateway.amazonaws.com"

  # Any stage, this method, this path with each parameter as a wildcard.
  source_arn = "${var.rest_api_execution_arn}/*/${var.http_method}${replace(aws_api_gateway_resource.resource.path, "/\\{[^}]+\\}/", "*")}"
}
