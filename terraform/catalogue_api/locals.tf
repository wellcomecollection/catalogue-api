locals {
  catalogue_vpcs = data.terraform_remote_state.accounts_catalogue.outputs

  vpc_id          = local.catalogue_vpcs["catalogue_vpc_id"]
  private_subnets = local.catalogue_vpcs["catalogue_vpc_private_subnets"]

  elastic_cloud_vpce_sg_id = data.terraform_remote_state.infra_critical.outputs["ec_catalogue_privatelink_sg_id"]

  // TODO move this tf state into this module
  search_api_repository = data.terraform_remote_state.catalogue_api_shared_old.outputs["ecr_search_repository_url"]
  items_api_repository  = data.terraform_remote_state.catalogue_api_shared_old.outputs["ecr_items_api_repository_url"]
}
