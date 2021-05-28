locals {
  catalogue_ec_cluster_id     = data.terraform_remote_state.catalogue_infra_critical.outputs.catalogue_ec_cluster_id
  catalogue_ec_cluster_name   = data.terraform_remote_state.catalogue_infra_critical.outputs.catalogue_ec_cluster_name
  catalogue_ec_cluster_ref_id = data.terraform_remote_state.catalogue_infra_critical.outputs.catalogue_ec_cluster_ref_id

  catalogue_ec_traffic_filter = [
    data.terraform_remote_state.infra_critical.outputs.ec_public_internet_traffic_filter_id,
    data.terraform_remote_state.infra_critical.outputs.ec_platform_privatelink_traffic_filter_id,
    data.terraform_remote_state.infra_critical.outputs.ec_catalogue_privatelink_traffic_filter_id
  ]

  default_tags = {
    Managed                   = "terraform"
    TerraformConfigurationURL = "https://github.com/wellcomecollection/catalogue-api/tree/main/terraform/shared"
  }

  logging_cluster_id = data.terraform_remote_state.infra_critical.outputs.logging_cluster_id
}
