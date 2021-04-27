module "catalogue_api_prod" {
  source = "../stack"

  environment_name = "prod"
  container_images = {
    search_api = "${local.search_api_repository}:env.prod"
    items_api = "${local.items_api_repository}:env.prod"
  }
  desired_task_counts = {
    search_api = 3
    items_api = 1
  }

  vpc_id = local.vpc_id
  private_subnets = local.private_subnets
  elastic_cloud_vpce_sg_id = local.elastic_cloud_vpce_sg_id
  cluster_arn = aws_ecs_cluster.catalogue_api.arn

  providers = {
    aws.dns = aws.dns
  }
}

module "catalogue_api_stage" {
  source = "../stack"

  environment_name = "stage"
  container_images = {
    search_api = "${local.search_api_repository}:env.stage"
    items_api = "${local.items_api_repository}:env.stage"
  }
  desired_task_counts = {
    search_api = 1
    items_api = 1
  }

  vpc_id = local.vpc_id
  private_subnets = local.private_subnets
  elastic_cloud_vpce_sg_id = local.elastic_cloud_vpce_sg_id
  cluster_arn = aws_ecs_cluster.catalogue_api.arn

  providers = {
    aws.dns = aws.dns
  }
}
