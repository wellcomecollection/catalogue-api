// The items v3 service, a Lambda. Its routes exist only in a stack that is
// given one, so stage can have them before prod does.
locals {
  items_v3_count = var.items_v3_lambda == null ? 0 : 1

  // Joins the deployment's triggers. Empty without the Lambda, which leaves
  // that stack's deployment as it was.
  items_v3_ids = concat(
    aws_api_gateway_resource.v3[*].id,
    aws_api_gateway_resource.v3_works[*].id,
    aws_api_gateway_resource.v3_single_work[*].id,
    aws_api_gateway_resource.management_items_v3[*].id,
    flatten(module.items_v3_route[*].all_ids),
    flatten(module.items_v3_manifest_route[*].all_ids),
  )
}

// /v3
resource "aws_api_gateway_resource" "v3" {
  count = local.items_v3_count

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_rest_api.catalogue.root_resource_id
  path_part   = "v3"
}

// /v3/works
resource "aws_api_gateway_resource" "v3_works" {
  count = local.items_v3_count

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v3[0].id
  path_part   = "works"
}

// /v3/works/{workId}
resource "aws_api_gateway_resource" "v3_single_work" {
  count = local.items_v3_count

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.v3_works[0].id
  path_part   = "{workId}"
}

// /v3/works/{workId}/items
module "items_v3_route" {
  count  = local.items_v3_count
  source = "../lambda_route"

  rest_api_id            = aws_api_gateway_rest_api.catalogue.id
  rest_api_execution_arn = aws_api_gateway_rest_api.catalogue.execution_arn
  parent_id              = aws_api_gateway_resource.v3_single_work[0].id
  path_part              = "items"
  http_method            = "GET"

  api_key_required = true

  lambda_name       = var.items_v3_lambda.name
  lambda_invoke_arn = var.items_v3_lambda.invoke_arn
}

// /management/items-v3/manifest
resource "aws_api_gateway_resource" "management_items_v3" {
  count = local.items_v3_count

  rest_api_id = aws_api_gateway_rest_api.catalogue.id
  parent_id   = aws_api_gateway_resource.management.id
  path_part   = "items-v3"
}

module "items_v3_manifest_route" {
  count  = local.items_v3_count
  source = "../lambda_route"

  rest_api_id            = aws_api_gateway_rest_api.catalogue.id
  rest_api_execution_arn = aws_api_gateway_rest_api.catalogue.execution_arn
  parent_id              = aws_api_gateway_resource.management_items_v3[0].id
  path_part              = "manifest"
  http_method            = "GET"

  lambda_name       = var.items_v3_lambda.name
  lambda_invoke_arn = var.items_v3_lambda.invoke_arn
}
