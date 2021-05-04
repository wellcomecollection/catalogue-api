locals {
  param_string = var.path_param == null ? "" : var.path_param

  method_request_parameters = {
    "method.request.path.${local.param_string}" = true
  }
  integration_request_parameters = {
    "integration.request.path.${local.param_string}" = "method.request.path.${local.param_string}"
  }
}

resource "aws_api_gateway_resource" "resource" {
  rest_api_id = var.rest_api_id
  parent_id   = var.parent_id
  path_part   = var.path_part
}

resource "aws_api_gateway_method" "method" {
  resource_id = aws_api_gateway_resource.resource.id

  rest_api_id        = var.rest_api_id
  http_method        = var.http_method
  request_parameters = var.path_param != null ? local.method_request_parameters : null

  authorization    = "NONE"
  api_key_required = var.api_key_required
}

resource "aws_api_gateway_integration" "integration" {
  rest_api_id = var.rest_api_id
  resource_id = aws_api_gateway_resource.resource.id

  http_method             = "ANY"
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  connection_type         = "VPC_LINK"
  connection_id           = var.vpc_link_id
  uri                     = "http://${var.external_hostname}:${var.lb_port}${var.integration_path}"
  request_parameters      = var.path_param != null ? local.integration_request_parameters : null
}
