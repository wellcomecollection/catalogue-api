locals {
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
