# No throttle_settings, so this plan imposes no limit of its own.
resource "aws_api_gateway_usage_plan" "internal" {
  name = "Identifiers API internal (${var.environment_name})"

  api_stages {
    api_id = aws_api_gateway_rest_api.identifiers.id
    stage  = aws_api_gateway_stage.default.stage_name
  }
}

# For consumers outside our control. These throttle values are a conservative
# placeholder until the load test sets real ones
# (https://github.com/wellcomecollection/platform/issues/6536).
resource "aws_api_gateway_usage_plan" "external" {
  name = "Identifiers API external (${var.environment_name})"

  api_stages {
    api_id = aws_api_gateway_rest_api.identifiers.id
    stage  = aws_api_gateway_stage.default.stage_name
  }

  throttle_settings {
    rate_limit  = 10
    burst_limit = 20
  }
}

resource "aws_api_gateway_api_key" "development" {
  name = "Identifiers API development (${var.environment_name})"
}

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

resource "aws_api_gateway_api_key" "items" {
  name = "Identifiers API items (${var.environment_name})"
}

resource "aws_api_gateway_usage_plan_key" "items" {
  key_id        = aws_api_gateway_api_key.items.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.internal.id
}

module "items_key_secret" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  key_value_map = {
    "identifiers_api/items/${var.environment_name}/api_key" = aws_api_gateway_api_key.items.value
  }
}

resource "aws_api_gateway_api_key" "requests" {
  name = "Identifiers API requests (${var.environment_name})"
}

resource "aws_api_gateway_usage_plan_key" "requests" {
  key_id        = aws_api_gateway_api_key.requests.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.internal.id
}

# The requests service runs in the identity account, so its secret is written
# there for the service to read by name.
module "requests_key_secret" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  providers = {
    aws = aws.identity
  }

  key_value_map = {
    "identifiers_api/requests/${var.environment_name}/api_key" = aws_api_gateway_api_key.requests.value
  }
}

resource "aws_api_gateway_api_key" "digirati" {
  name = "Identifiers API digirati (${var.environment_name})"
}

resource "aws_api_gateway_usage_plan_key" "digirati" {
  key_id        = aws_api_gateway_api_key.digirati.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.external.id
}

module "digirati_key_secret" {
  source = "github.com/wellcomecollection/terraform-aws-secrets.git?ref=v1.2.0"

  providers = {
    aws = aws.digirati
  }

  # Prefixed with wellcome/ because Digirati control this account, matching the
  # secrets the identity repo writes there.
  key_value_map = {
    "wellcome/identifiers_api/digirati/${var.environment_name}/api_key" = aws_api_gateway_api_key.digirati.value
  }
}
