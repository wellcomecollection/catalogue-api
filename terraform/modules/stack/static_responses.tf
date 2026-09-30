locals {
  v1_deprecation_string = "This API is now decommissioned. Please use https://api.wellcomecollection.org/catalogue/v2/works."
  v1_gone_body = {
    errorType   = "http",
    httpStatus  = 410,
    label       = "Gone",
    description = local.v1_deprecation_string
    type        = "Error",
  }
}

module "gateway_responses" {
  source = "github.com/wellcomecollection/terraform-aws-api-gateway-responses.git?ref=v1.2.0"

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

// The shared module owns these types now. Deleting them would reset the module's copy, and
// forgetting them cycles with the deployment's create_before_destroy, so `terraform state rm`
// them before applying; these blocks make a plan that skips that step fail rather than delete.
removed {
  from = aws_api_gateway_gateway_response.not_found_404
  lifecycle {
    destroy = false
  }
}

removed {
  from = aws_api_gateway_gateway_response.no_resource
  lifecycle {
    destroy = false
  }
}
