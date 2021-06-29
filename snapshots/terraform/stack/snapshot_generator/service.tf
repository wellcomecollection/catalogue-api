module "snapshot_generator" {
  source = "../../modules/worker"

  name  = "snapshot_generator-${var.deployment_service_env}"
  image = "${var.snapshot_generator_image}:env.${var.deployment_service_env}"

  env_vars = {
    queue_url        = module.snapshot_generator_input_queue.url
    topic_arn        = module.snapshot_generator_output_topic.arn
    metric_namespace = "snapshot_generator-${var.deployment_service_env}"
    es_bulk_size     = var.es_bulk_size
  }

  cpu    = 4096
  memory = 8192

  subnets = var.subnets

  cluster_name = var.cluster_name
  cluster_arn  = var.cluster_arn

  security_group_ids = [
    # TODO: Do we need this egress security group?
    aws_security_group.egress.id,
  ]
  elastic_cloud_vpce_sg_id = var.elastic_cloud_vpce_sg_id

  min_capacity = 0
  max_capacity = 1

  deployment_service_env  = var.deployment_service_env
  deployment_service_name = "snapshot-generator"
  shared_logging_secrets  = var.shared_logging_secrets
}

module "snapshot_generator_scaling_alarm" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//autoscaling?ref=v1.1.3"
  queue_name = module.snapshot_generator_input_queue.name

  queue_high_actions = [module.snapshot_generator.scale_up_arn]
  queue_low_actions  = [module.snapshot_generator.scale_down_arn]
}
