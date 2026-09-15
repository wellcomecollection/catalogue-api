# No throttle_settings, so this plan imposes no limit of its own.
resource "aws_api_gateway_usage_plan" "internal" {
  name = "Identifiers API internal (${var.environment_name})"

  api_stages {
    api_id = aws_api_gateway_rest_api.identifiers.id
    stage  = aws_api_gateway_stage.default.stage_name
  }
}

resource "aws_api_gateway_api_key" "development" {
  name = "Identifiers API development (${var.environment_name})"
}

# A key is only accepted if it reaches a stage through a usage plan, so this
# association is what makes the key usable, not the key resource on its own.
resource "aws_api_gateway_usage_plan_key" "development" {
  key_id        = aws_api_gateway_api_key.development.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.internal.id
}

module "development_key_secret" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  key_value_map = {
    "identifiers_api/development/${var.environment_name}/api_key" = aws_api_gateway_api_key.development.value
  }
}
