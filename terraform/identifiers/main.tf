locals {
  registry = data.terraform_remote_state.infra_critical.outputs["id_minter_rds"]["2026-07-03"]
}

data "aws_ecr_image" "identifiers" {
  repository_name = "uk.ac.wellcome/identifiers"
  most_recent     = true
}

module "identifiers_stage" {
  source = "../modules/identifiers"

  environment_name  = "stage"
  openapi_spec_path = "${path.root}/../../identifiers/spec/openapi.yaml"

  # Digest-pinned. terraform-aws-lambda ignores changes, so this only sets the
  # image the Lambda is created with; the deploy workflow moves it after that.
  image_uri = data.aws_ecr_image.identifiers.image_uri

  registry_cluster_arn   = local.registry["cluster_arn"]
  registry_read_role_arn = local.registry["identifiers_api_read_role_arn"]
  registry_secret_arn    = local.registry["identifiers_api_read_secret_arn"]

  providers = {
    aws.dns = aws.dns
  }
}
