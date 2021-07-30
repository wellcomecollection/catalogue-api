resource "aws_api_gateway_usage_plan" "items_api" {
  name = "Items API (${var.environment_name})"

  api_stages {
    api_id = aws_api_gateway_rest_api.catalogue.id
    stage  = aws_api_gateway_stage.default.stage_name
  }
}

resource "aws_api_gateway_api_key" "items_api" {
  name = "Items API (${var.environment_name})"
}

resource "aws_api_gateway_usage_plan_key" "items_api" {
  key_id        = aws_api_gateway_api_key.items_api.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.items_api.id
}

module "items_api_key_secret" {
  source = "github.com/wellcomecollection/terraform-aws-ecs-service.git//modules/secrets"

  providers = {
    aws = aws.experience
  }

  key_value_map = {
    "catalogue_api/items/${var.environment_name}/api_key" = aws_api_gateway_api_key.items_api.value
  }
}
