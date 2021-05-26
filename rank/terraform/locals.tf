locals {
  catalogue_ec_cluster_id     = data.terraform_remote_state.catalogue_infra_critical.outputs.catalogue_ec_cluster_id
  catalogue_ec_cluster_name   = data.terraform_remote_state.catalogue_infra_critical.outputs.catalogue_ec_cluster_name
  catalogue_ec_cluster_ref_id = data.terraform_remote_state.catalogue_infra_critical.outputs.catalogue_ec_cluster_ref_id
}
