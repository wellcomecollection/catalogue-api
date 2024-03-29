locals {
  catalogue_vpcs = data.terraform_remote_state.accounts_catalogue.outputs

  vpc_id          = local.catalogue_vpcs["catalogue_vpc_id"]
  private_subnets = local.catalogue_vpcs["catalogue_vpc_private_subnets"]

  elastic_cloud_vpce_sg_id = data.terraform_remote_state.infra_critical.outputs["ec_catalogue_privatelink_sg_id"]

  search_repository   = data.terraform_remote_state.catalogue_api_shared.outputs["ecr_search_repository_url"]
  items_repository    = data.terraform_remote_state.catalogue_api_shared.outputs["ecr_items_repository_url"]
  concepts_repository = data.terraform_remote_state.catalogue_api_shared.outputs["ecr_concepts_repository_url"]

  apm_secret_config = {
    apm_server_url = "catalogue/api/apm_server_url"
    apm_secret     = "catalogue/api/apm_secret"
  }

  // TODO: Requests & Items APIs have different security profile
  // TODO: Requests will access PII - and must have a different set of credentials!
  // See: https://github.com/wellcomecollection/catalogue-api/issues/95
  sierra_secret_config = {
    sierra_api_key    = "stacks/prod/sierra_api_key"
    sierra_api_secret = "stacks/prod/sierra_api_secret"
  }
}
