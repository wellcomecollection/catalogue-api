locals {
  # The registry-read role's trust policy names lambda-role-identifiers-api-stage,
  # which terraform-aws-lambda derives from this name. Changing it breaks the
  # cross-account read.
  lambda_name = "identifiers-api-${var.environment_name}"
}

module "identifiers_lambda" {
  source = "github.com/wellcomecollection/terraform-aws-lambda?ref=v1.2.0"

  name         = local.lambda_name
  description  = "Identifiers API (${var.environment_name})"
  package_type = "Image"
  image_uri    = var.image_uri

  # Two indexed lookups, so anything approaching this is stuck rather than slow.
  timeout     = 10
  memory_size = 512

  environment = {
    variables = {
      IDENTIFIERS_BACKEND = "rds"
      RDS_RESOURCE_ARN    = var.registry_cluster_arn
      RDS_SECRET_ARN      = var.registry_secret_arn
      RDS_ASSUME_ROLE_ARN = var.registry_read_role_arn
    }
  }
}

resource "aws_iam_role_policy" "assume_registry_read" {
  name   = "${local.lambda_name}-assume-registry-read"
  role   = module.identifiers_lambda.lambda_role.name
  policy = data.aws_iam_policy_document.assume_registry_read.json
}

data "aws_iam_policy_document" "assume_registry_read" {
  statement {
    actions   = ["sts:AssumeRole"]
    resources = [var.registry_read_role_arn]
  }
}
