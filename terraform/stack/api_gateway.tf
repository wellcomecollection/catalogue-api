resource "aws_api_gateway_rest_api" "catalogue" {
  name = "Catalogue API (${var.environment_name})"

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_deployment" "default" {
  rest_api_id = aws_api_gateway_rest_api.catalogue.id

  triggers = {
    // This sometimes causes a deploy to occur before resources have been updated
    // This is mentioned in the docs but it's unclear to me how to resolve it;
    // since we don't expect the gateway config to change often it's been left as-is
    // with the proviso that sometimes a manual deploy from the console is necessary
    // https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/apigatewayv2_deployment#redeployment-triggers
    redeployment = filesha1("${path.module}/api_gateway.tf")
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
  source = "../modules/api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v2.id
  path_part   = "works"
  http_method = "ANY"

  integration_path = "/catalogue/v2/works"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/images
module "images_route" {
  source = "../modules/api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v2.id
  path_part   = "images"
  http_method = "ANY"

  integration_path = "/catalogue/v2/images"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/images/{imageId}
module "single_image_route" {
  source = "../modules/api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.images_route.resource_id
  path_part   = "{imageId}"
  http_method = "ANY"

  path_param       = "imageId"
  integration_path = "/catalogue/v2/images/{imageId}"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/works/{workId}
module "single_work_route" {
  source = "../modules/api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.works_route.resource_id
  path_part   = "{workId}"
  http_method = "ANY"

  path_param       = "workId"
  integration_path = "/catalogue/v2/works/{workId}"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// /v2/works/{workId}/items
module "items_route" {
  source = "../modules/api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = module.single_work_route.resource_id
  path_part   = "items"
  http_method = "ANY"

  path_param       = "workId"
  integration_path = "/works/{workId}"
  lb_port          = local.items_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}

// default
module "default_route" {
  source = "../modules/api_route"

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_rest_api.catalogue.root_resource_id
  path_part   = "{proxy+}"
  http_method = "ANY"

  path_param       = "proxy"
  integration_path = "/catalogue/{proxy}"
  lb_port          = local.search_lb_port

  vpc_link_id       = aws_api_gateway_vpc_link.catalogue_lb.id
  external_hostname = var.external_hostname
}
