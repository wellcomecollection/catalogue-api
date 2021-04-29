resource "aws_api_gateway_resource" "static" {
  rest_api_id = var.rest_api_id
  parent_id   = var.parent_id
  path_part   = var.path_part
}

resource "aws_api_gateway_method" "static" {
  rest_api_id   = var.rest_api_id
  resource_id   = aws_api_gateway_resource.static.id
  authorization = "NONE"
  http_method   = var.http_method
}

resource "aws_api_gateway_method_response" "static" {
  rest_api_id = var.rest_api_id
  resource_id = aws_api_gateway_resource.static.id
  http_method = aws_api_gateway_method.static.http_method
  status_code = var.status_code
}

resource "aws_api_gateway_integration" "static" {
  rest_api_id          = var.rest_api_id
  resource_id          = aws_api_gateway_resource.static.id
  http_method          = aws_api_gateway_method.static.http_method
  type                 = "MOCK"
  passthrough_behavior = "WHEN_NO_TEMPLATES"

  request_templates = {
    "application/json" = jsonencode({
      statusCode = var.status_code
    })
  }
}

resource "aws_api_gateway_integration_response" "static" {
  rest_api_id = var.rest_api_id
  resource_id = aws_api_gateway_resource.static.id
  http_method = aws_api_gateway_method.static.http_method
  status_code = aws_api_gateway_method_response.static.status_code

  response_templates = {
    "application/json" = var.body
  }

  depends_on = [aws_api_gateway_integration.static]
}
