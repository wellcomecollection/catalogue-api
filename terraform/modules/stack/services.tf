locals {
  ecs_security_groups = [
    aws_security_group.lb_ingress.id,
    aws_security_group.egress.id,
    var.elastic_cloud_vpce_sg_id
  ]

  container_ports = {
    concepts = 7777
    search   = 8888
    items    = 9999
  }
}

locals {
  catalogue_api_public_root = "https://${var.external_hostname}/catalogue/v2"
  content_api_public_root   = "https://${var.external_hostname}/content/v0"
}

module "search_api" {
  source       = "../service"
  service_name = "${var.environment_name}-search-api"

  container_port              = local.container_ports.search
  container_image             = var.container_images.search
  desired_task_count          = var.desired_task_counts.search
  load_balancer_listener_port = local.search_lb_port

  environment = {
    app_port         = local.container_ports.search
    app_base_url     = local.catalogue_api_public_root
    api_public_root  = local.catalogue_api_public_root
    apm_service_name = "search-api"
    apm_environment  = var.environment_name

    metrics_namespace = "search-api"
  }

  app_cpu    = var.environment_name == "prod" ? 1024 : 512
  app_memory = var.environment_name == "prod" ? 2048 : 1024

  secrets = var.apm_secret_config

  # Below this line is boilerplate that should be the same across
  # all Fargate services.
  subnets            = local.routable_private_subnets
  security_group_ids = local.ecs_security_groups
  cluster_arn        = var.cluster_arn
  vpc_id             = var.vpc_id
  load_balancer_arn  = aws_lb.catalogue_api.arn

  use_fargate_spot              = var.environment_name == "stage" ? true : false
  turn_off_outside_office_hours = var.environment_name == "stage" ? true : false
}

module "items_api" {
  source       = "../service"
  service_name = "${var.environment_name}-items-api"

  container_port              = local.container_ports.items
  container_image             = var.container_images.items
  desired_task_count          = var.desired_task_counts.items
  load_balancer_listener_port = local.items_lb_port

  environment = {
    app_port           = local.container_ports.items
    app_base_url       = "https://${var.external_hostname}/stacks/v1/items"
    catalogue_base_url = "https://${var.external_hostname}/catalogue/v2"
    sierra_base_url    = "https://libsys.wellcomelibrary.org/iii/sierra-api"

    catalogue_api_public_root = local.catalogue_api_public_root
    content_api_public_root   = local.content_api_public_root

    api_host         = var.external_hostname
    apm_service_name = "items-api"
    apm_environment  = var.environment_name

    log_level         = "INFO"
    metrics_namespace = "items-api"
  }

  secrets = merge(var.apm_secret_config, var.sierra_secret_config)

  app_cpu    = 256
  app_memory = 512

  # Below this line is boilerplate that should be the same across
  # all Fargate services.
  subnets            = local.routable_private_subnets
  security_group_ids = local.ecs_security_groups
  cluster_arn        = var.cluster_arn
  vpc_id             = var.vpc_id
  load_balancer_arn  = aws_lb.catalogue_api.arn

  use_fargate_spot              = var.environment_name == "stage" ? true : false
  turn_off_outside_office_hours = var.environment_name == "stage" ? true : false
}

module "concepts_api" {
  source       = "../service"
  service_name = "${var.environment_name}-concepts-api"

  container_port              = local.container_ports.concepts
  container_image             = var.container_images.concepts
  desired_task_count          = var.desired_task_counts.concepts
  load_balancer_listener_port = local.concepts_lb_port

  environment = {
    PORT            = local.container_ports.concepts
    PUBLIC_ROOT_URL = local.catalogue_api_public_root

    apm_service_name = "concepts-api"
    apm_environment  = var.environment_name
  }

  secrets = var.apm_secret_config

  app_cpu    = var.environment_name == "prod" ? 512 : 256
  app_memory = var.environment_name == "prod" ? 1024 : 512

  # Below this line is boilerplate that should be the same across
  # all Fargate services.
  subnets            = local.routable_private_subnets
  security_group_ids = local.ecs_security_groups
  cluster_arn        = var.cluster_arn
  vpc_id             = var.vpc_id
  load_balancer_arn  = aws_lb.catalogue_api.arn

  use_fargate_spot              = var.environment_name == "stage" ? true : false
  turn_off_outside_office_hours = var.environment_name == "stage" ? true : false
}
