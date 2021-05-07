locals {
  ecs_security_groups = [
    aws_security_group.lb_ingress.id,
    aws_security_group.egress.id,
    var.elastic_cloud_vpce_sg_id
  ]

  container_ports = {
    search = 8888
    items  = 9999
  }
}

module "search_api" {
  source                  = "../service"
  service_name            = "${var.environment_name}-search-api"
  deployment_service_name = "search-api"

  container_port              = local.container_ports.search
  container_image             = var.container_images.search
  desired_task_count          = var.desired_task_counts.search
  load_balancer_listener_port = local.search_lb_port
  security_group_ids          = local.ecs_security_groups

  environment = {
    app_port         = local.container_ports.search
    api_public_root  = "https://${var.external_hostname}/catalogue/v2"
    apm_service_name = "search-api"
    apm_environment  = var.environment_name
  }
  secrets = {
    es_host        = "elasticsearch/catalogue/private_host"
    es_port        = "catalogue/api/es_port"
    es_protocol    = "catalogue/api/es_protocol"
    es_username    = "catalogue/api/es_username"
    es_password    = "catalogue/api/es_password"
    apm_server_url = "catalogue/api/apm_server_url"
    apm_secret     = "catalogue/api/apm_secret"
  }

  subnets                = local.routable_private_subnets
  cluster_arn            = var.cluster_arn
  vpc_id                 = var.vpc_id
  load_balancer_arn      = aws_lb.catalogue_api.arn
  deployment_service_env = var.environment_name
}

module "items_api" {
  source                  = "../service"
  service_name            = "${var.environment_name}-items-api"
  deployment_service_name = "items-api"

  container_port              = local.container_ports.items
  container_image             = var.container_images.items
  desired_task_count          = var.desired_task_counts.items
  load_balancer_listener_port = local.items_lb_port
  security_group_ids          = local.ecs_security_groups

  environment = {
    app_port           = local.container_ports.items
    app_base_url       = "https://${var.external_hostname}/stacks/v1/items"
    context_url        = "https://${var.external_hostname}/stacks/v1/context.json"
    catalogue_base_url = "https://${var.external_hostname}/catalogue/v2"
    sierra_base_url    = "https://libsys.wellcomelibrary.org/iii/sierra-api"

    api_host         = var.external_hostname
    apm_service_name = "items-api"
    apm_environment  = var.environment_name

    log_level         = "DEBUG"
    metrics_namespace = "items-api"
  }
  secrets = {
    sierra_api_key    = "stacks/prod/sierra_api_key"
    sierra_api_secret = "stacks/prod/sierra_api_secret"

    apm_server_url = "catalogue/api/apm_server_url"
    apm_secret     = "catalogue/api/apm_secret"

    es_host        = "elasticsearch/catalogue/private_host"
    es_port        = "catalogue/api/es_port"
    es_protocol    = "catalogue/api/es_protocol"
    es_username    = "catalogue/api/es_username"
    es_password    = "catalogue/api/es_password"
  }

  subnets                = local.routable_private_subnets
  cluster_arn            = var.cluster_arn
  vpc_id                 = var.vpc_id
  load_balancer_arn      = aws_lb.catalogue_api.arn
  deployment_service_env = var.environment_name
}

