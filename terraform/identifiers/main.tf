locals {
  id_minter_rds = data.terraform_remote_state.infra_critical.outputs["id_minter_rds"]

  # Which registry each environment reads. Both read the 2026-07-03 registry,
  # which became the production registry at the Axiell switchover.
  prod_registry  = local.id_minter_rds["2026-07-03"]
  stage_registry = local.id_minter_rds["2026-07-03"]
}

# Digest-pinned. terraform-aws-lambda ignores image_uri changes, so this only
# sets the image each Lambda is created with; the deploy workflow moves it after.
data "aws_ecr_image" "identifiers" {
  repository_name = "uk.ac.wellcome/identifiers"
  most_recent     = true
}

module "identifiers_prod" {
  source = "../modules/identifiers"

  environment_name  = "prod"
  openapi_spec_path = "${path.root}/../../identifiers/spec/openapi.yaml"

  image_uri = data.aws_ecr_image.identifiers.image_uri

  registry_cluster_arn   = local.prod_registry["cluster_arn"]
  registry_read_role_arn = local.prod_registry["identifiers_api_read_role_arn"]
  registry_secret_arn    = local.prod_registry["identifiers_api_read_secret_arn"]

  providers = {
    aws.dns = aws.dns
  }
}

module "identifiers_stage" {
  source = "../modules/identifiers"

  environment_name  = "stage"
  openapi_spec_path = "${path.root}/../../identifiers/spec/openapi.yaml"

  image_uri = data.aws_ecr_image.identifiers.image_uri

  registry_cluster_arn   = local.stage_registry["cluster_arn"]
  registry_read_role_arn = local.stage_registry["identifiers_api_read_role_arn"]
  registry_secret_arn    = local.stage_registry["identifiers_api_read_secret_arn"]

  providers = {
    aws.dns = aws.dns
  }
}
