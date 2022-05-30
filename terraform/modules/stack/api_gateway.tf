resource "aws_api_gateway_rest_api" "catalogue" {
  name = "Catalogue API (${var.environment_name})"

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

// This is quite tricky, context for the way it's configured can be found here:
// https://github.com/hashicorp/terraform-provider-aws/issues/11344#issuecomment-765138213
// If you're seeing issues with changes not appearing (or even disappearing), then
// manually deploying resources via the AWS Console can often help.
resource "aws_api_gateway_deployment" "default" {
  rest_api_id = aws_api_gateway_rest_api.catalogue.id

  triggers = {
    // IMPORTANT: New resources need to be added here manually (see above)
    redeployment = sha1(jsonencode(concat(
      [
        aws_api_gateway_resource.v2.id,
        aws_api_gateway_gateway_response.no_resource.id,
        aws_api_gateway_gateway_response.not_found_404.id,
        module.gateway_responses.fingerprint,
      ],
      module.works_route.all_ids,
      module.images_route.all_ids,
      module.single_image_route.all_ids,
      module.single_work_route.all_ids,
      module.items_route.all_ids,
      module.concepts_route.all_ids,
      module.single_concept_route.all_ids
      module.default_route.all_ids,
      module.v1_root_gone.all_ids,
      module.v1_gone.all_ids
    )))
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_api_gateway_stage" "default" {
  rest_api_id   = aws_api_gateway_rest_api.catalogue.id
  deployment_id = aws_api_gateway_deployment.default.id
  stage_name    = "default"
}

resource "aws_api_gateway_vpc_link" "catalogue_lb" {
  name        = "${var.environment_name}-lb-link"
  target_arns = [aws_lb.catalogue_api.arn]

  lifecycle {
    create_before_destroy = true
  }
}

// /v2
resource "aws_api_gateway_resource" "v2" {
  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_rest_api.catalogue.root_resource_id
  path_part   = "v2"
}

// /v2/works
module "works_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v2.id
  path_part   = "works"
  http_method = "ANY"

  integration_path = "/works"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/images
module "images_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v2.id
  path_part   = "images"
  http_method = "ANY"

  integration_path = "/images"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/images/{imageId}
module "single_image_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.images_route.resource_id
  path_part   = "{imageId}"
  http_method = "ANY"

  path_param       = "imageId"
  integration_path = "/images/{imageId}"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/works/{workId}
module "single_work_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.works_route.resource_id
  path_part   = "{workId}"
  http_method = "ANY"

  path_param       = "workId"
  integration_path = "/works/{workId}"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/works/{workId}/items
module "items_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.single_work_route.resource_id
  path_part   = "items"
  http_method = "ANY"

  api_key_required = true

  path_param       = "workId"
  integration_path = "/works/{workId}"
  lb_port          = local.items_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/concepts
module "concepts_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v2.id
  path_part   = "concepts"
  http_method = "ANY"

  integration_path = "/concepts"
  lb_port          = local.concepts_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/concepts/{conceptId}
module "single_concept_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.concepts_route.resource_id
  path_part   = "{conceptId}"
  http_method = "ANY"

  path_param       = "conceptId"
  integration_path = "/concepts/{conceptId}"
  lb_port          = local.concepts_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// default
module "default_route" {
  source = "../api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v2.id
  path_part   = "{proxy+}"
  http_method = "ANY"

  path_param       = "proxy"
  integration_path = "/{proxy}"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}
