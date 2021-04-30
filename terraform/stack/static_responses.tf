locals {
  v1_deprecation_string = "This API is now decommissioned. Please use https://api.wellcomecollection.org/catalogue/v2/works."
  v1_gone_body = jsonencode({
    errorType   = "http",
    httpStatus  = 410,
    label       = "Gone",
    description = local.v1_deprecation_string
    type        = "Error",
    "@context"  = "https://api.wellcomecollection.org/catalogue/v2/context.json"
  })
}

module "v1_root_gone" {
  source = "../modules/static_response"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_rest_api.catalogue.root_resource_id
  path_part   = "v1"

  http_method = "GET"
  status_code = 410
  body        = local.v1_gone_body
}

module "v1_gone" {
  source = "../modules/static_response"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.v1_root_gone.resource_id
  path_part   = "{proxy+}"

  http_method = "GET"
  status_code = 410
  body        = local.v1_gone_body
}
