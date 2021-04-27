locals {
  search_lb_port = 8000
  items_lb_port  = 8001

  external_hostname = "api.wellcomecollection.org"
  version_path_part = "v2"

  // TODO remove delta
  api_gateway_domain_name = var.environment_name != "stage" ? "catalogue.api-${var.environment_name}.wellcomecollection.org" : "catalogue.api-${var.environment_name}-delta.wellcomecollection.org"

  routable_private_subnets = slice(var.private_subnets, 0, min(var.desired_task_counts.search_api, var.desired_task_counts.items_api, length(var.private_subnets)))
}
