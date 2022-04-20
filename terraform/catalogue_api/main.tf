module "catalogue_api_prod" {
  source = "../modules/stack"

  environment_name  = "prod"
  external_hostname = "api.wellcomecollection.org"

  container_images = {
    search = "${local.search_repository}:env.prod"
    items  = "${local.items_repository}:env.prod"
  }

  desired_task_counts = {
    search = 3
    items  = 3
  }

  vpc_id                   = local.vpc_id
  private_subnets          = local.private_subnets
  elastic_cloud_vpce_sg_id = local.elastic_cloud_vpce_sg_id
  cluster_arn              = aws_ecs_cluster.catalogue_api.arn

  apm_secret_config    = local.apm_secret_config
  sierra_secret_config = local.sierra_secret_config

  api_gateway_alerts_topic_arn = local.api_gateway_alerts_topic_arn

  providers = {
    aws.dns        = aws.dns
    aws.experience = aws.experience
  }
}

module "catalogue_api_stage" {
  source = "../modules/stack"

  environment_name  = "stage"
  external_hostname = "api-stage.wellcomecollection.org"

  container_images = {
    search = "${local.search_repository}:env.stage"
    items  = "${local.items_repository}:env.stage"
  }

  desired_task_counts = {
    search = 1
    items  = 1
  }

  vpc_id                   = local.vpc_id
  private_subnets          = local.private_subnets
  elastic_cloud_vpce_sg_id = local.elastic_cloud_vpce_sg_id
  cluster_arn              = aws_ecs_cluster.catalogue_api.arn

  apm_secret_config    = local.apm_secret_config
  sierra_secret_config = local.sierra_secret_config

  api_gateway_alerts_topic_arn = local.api_gateway_alerts_topic_arn

  providers = {
    aws.dns        = aws.dns
    aws.experience = aws.experience
  }
}
