locals {
  v1_deprecation_string = "This API is now decommissioned. Please use https://api.wellcomecollection.org/catalogue/v2/works."
  v1_gone_body = {
    errorType   = "http",
    httpStatus  = 410,
    label       = "Gone",
    description = local.v1_deprecation_string
    type        = "Error",
  }

  not_found_body = {
    errorType   = "http",
    httpStatus  = 404,
    label       = "Not Found",
    description = "Page not found for URL $context.path"
    type        = "Error",
  }
}

module "gateway_responses" {
  source = "github.com/wellcomecollection/terraform-aws-api-gateway-responses.git?ref=v1.1.1"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
}

module "v1_root_gone" {
  source = "../static_response"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_rest_api.catalogue.root_resource_id
  path_part   = "v1"

  http_method = "GET"
  status_code = 410
  body        = jsonencode(local.v1_gone_body)
}

module "v1_gone" {
  source = "../static_response"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.v1_root_gone.resource_id
  path_part   = "{proxy+}"

  http_method = "GET"
  status_code = 410
  body        = jsonencode(local.v1_gone_body)
}

resource "aws_api_gateway_gateway_response" "not_found_404" {
  rest_api_id   = aws_api_gateway_rest_api.catalogue.id
  response_type = "RESOURCE_NOT_FOUND"
  status_code   = "404"

  response_templates = {
    "application/json" = jsonencode(local.not_found_body)
  }
}

resource "aws_api_gateway_gateway_response" "no_resource" {
  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  // This is confusingly named, it's used when there isn't a method/resource too
  response_type = "MISSING_AUTHENTICATION_TOKEN"
  status_code   = "404"

  response_templates = {
    "application/json" = jsonencode(local.not_found_body)
  }
}
