data "aws_s3_bucket" "public_data" {
  bucket = "wellcomecollection-data-public-delta"
}

locals {
  cluster_arn  = data.terraform_remote_state.data_api.outputs.cluster_arn
  cluster_name = data.terraform_remote_state.data_api.outputs.cluster_name

  public_object_key_v2    = "catalogue/v2/works.json.gz"
  public_data_bucket_name = data.aws_s3_bucket.public_data.id

  snapshot_generator_image = data.terraform_remote_state.api_shared.outputs.ecr_snapshot_generator_repository_url

  shared_logging_secrets = data.terraform_remote_state.shared.outputs.shared_secrets_logging

  monitoring_outputs = data.terraform_remote_state.monitoring.outputs

  lambda_error_alarm_arn = local.monitoring_outputs["catalogue_lambda_error_alerts_topic_arn"]
  dlq_alarm_arn          = local.monitoring_outputs["catalogue_dlq_alarm_topic_arn"]

  vpc_id  = data.terraform_remote_state.catalogue_account.outputs.catalogue_vpc_id
  subnets = data.terraform_remote_state.catalogue_account.outputs.catalogue_vpc_private_subnets

  elastic_cloud_vpce_sg_id = data.terraform_remote_state.shared.outputs["ec_catalogue_privatelink_sg_id"]
}
