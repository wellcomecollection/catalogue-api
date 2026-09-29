locals {
  openapi_body = templatefile(var.openapi_spec_path, {
    identifiers_lambda_invoke_arn = module.identifiers_lambda.lambda.invoke_arn
  })
}

resource "aws_api_gateway_rest_api" "identifiers" {
  name = "Identifiers API (${var.environment_name})"
  body = local.openapi_body

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_deployment" "default" {
  rest_api_id = aws_api_gateway_rest_api.identifiers.id

  triggers = {
    redeployment = sha1(local.openapi_body)
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "default" {
  rest_api_id   = aws_api_gateway_rest_api.identifiers.id
  deployment_id = aws_api_gateway_deployment.default.id
  stage_name    = "default"

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.access_logs.arn
    format = jsonencode({
      requestId          = "$context.requestId"
      apiKeyId           = "$context.identity.apiKeyId"
      httpMethod         = "$context.httpMethod"
      resourcePath       = "$context.resourcePath"
      status             = "$context.status"
      errorType          = "$context.error.responseType"
      responseLatency    = "$context.responseLatency"
      integrationLatency = "$context.integrationLatency"
      requestTime        = "$context.requestTime"
    })
  }
}

resource "aws_lambda_permission" "api_gateway" {
  action        = "lambda:InvokeFunction"
  function_name = module.identifiers_lambda.lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.identifiers.execution_arn}/*/*"
}

resource "aws_cloudwatch_log_group" "access_logs" {
  name              = "/aws/apigateway/identifiers-api-${var.environment_name}"
  retention_in_days = 30
}
