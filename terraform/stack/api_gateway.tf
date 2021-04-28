resource "aws_apigatewayv2_api" "catalogue" {
  name          = "Catalogue API (${var.environment_name})"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_deployment" "default" {
  api_id = aws_apigatewayv2_api.catalogue.id

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

resource "aws_apigatewayv2_stage" "default" {
  api_id        = aws_apigatewayv2_api.catalogue.id
  deployment_id = aws_apigatewayv2_deployment.default.id
  name          = "$default"
}

resource "aws_apigatewayv2_vpc_link" "catalogue" {
  name               = "${var.environment_name}-catalogue-api"
  security_group_ids = []
  subnet_ids         = local.routable_private_subnets

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_apigatewayv2_integration" "search_integration" {
  api_id          = aws_apigatewayv2_api.catalogue.id
  connection_id   = aws_apigatewayv2_vpc_link.catalogue.id
  integration_uri = module.search_api.lb_listener_arn
  description     = "Search API"

  integration_type   = "HTTP_PROXY"
  connection_type    = "VPC_LINK"
  integration_method = "ANY"
}

resource "aws_apigatewayv2_integration" "items_integration" {
  api_id          = aws_apigatewayv2_api.catalogue.id
  connection_id   = aws_apigatewayv2_vpc_link.catalogue.id
  integration_uri = module.items_api.lb_listener_arn
  description     = "Items API"
  request_parameters = {
    "overwrite:path" = "/works/$request.path.workId"
  }


  integration_type   = "HTTP_PROXY"
  connection_type    = "VPC_LINK"
  integration_method = "ANY"
}

locals {
  search_target = "integrations/${aws_apigatewayv2_integration.search_integration.id}"
  items_target  = "integrations/${aws_apigatewayv2_integration.items_integration.id}"
}

// /v2/works/
resource "aws_apigatewayv2_route" "works" {
  api_id    = aws_apigatewayv2_api.catalogue.id
  route_key = "ANY /catalogue/v2/works"
  target    = local.search_target
}

// /v2/works/{workId}
resource "aws_apigatewayv2_route" "single_work" {
  api_id    = aws_apigatewayv2_api.catalogue.id
  route_key = "ANY /catalogue/v2/works/{workId}"
  target    = local.search_target
}

// /v2/works/{workId}/items
resource "aws_apigatewayv2_route" "items" {
  api_id    = aws_apigatewayv2_api.catalogue.id
  route_key = "ANY /catalogue/v2/works/{workId}/items"
  target    = local.items_target
}

// /v2/images/*
resource "aws_apigatewayv2_route" "images" {
  api_id    = aws_apigatewayv2_api.catalogue.id
  route_key = "ANY /catalogue/v2/images/{proxy+}"
  target    = local.search_target
}

// default
resource "aws_apigatewayv2_route" "default" {
  api_id    = aws_apigatewayv2_api.catalogue.id
  route_key = "$default"
  target    = local.search_target
}

