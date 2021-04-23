module "snapshot_generator" {
  source     = "./snapshot_generator"
  aws_region = var.aws_region

  cluster_arn  = var.cluster_arn
  cluster_name = var.cluster_name

  snapshot_generator_image = var.snapshot_generator_image
  deployment_service_env   = var.deployment_service_env

  es_bulk_size = var.es_bulk_size

  public_bucket_name = var.public_bucket_name

  snapshot_generator_input_topic_arn = module.snapshot_scheduler.topic_arn

  shared_logging_secrets = var.shared_logging_secrets

  dlq_alarm_arn = var.dlq_alarm_arn

  elastic_cloud_vpce_sg_id = var.elastic_cloud_vpce_sg_id

  vpc_id  = var.vpc_id
  subnets = var.subnets
}

module "snapshot_scheduler" {
  source = "./snapshot_scheduler"

  deployment_service_env = var.deployment_service_env

  lambda_upload_bucket = var.lambda_upload_bucket

  lambda_error_alarm_arn = var.lambda_error_alarm_arn

  public_bucket_name   = var.public_bucket_name
  public_object_key_v2 = var.public_object_key_v2
}

module "snapshot_recorder" {
  source = "./snapshot_recorder"

  snapshot_generator_output_topic_arn = module.snapshot_generator.output_topic_arn

  deployment_service_env = var.deployment_service_env

  lambda_upload_bucket   = var.lambda_upload_bucket
  lambda_error_alarm_arn = var.lambda_error_alarm_arn
}

module "snapshot_reporter" {
  source = "./snapshot_reporter"

  deployment_service_env = var.deployment_service_env

  lambda_upload_bucket   = var.lambda_upload_bucket
  lambda_error_alarm_arn = var.lambda_error_alarm_arn
}
