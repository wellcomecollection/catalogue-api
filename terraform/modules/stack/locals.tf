locals {
  search_lb_port    = 8000
  items_lb_port     = 8001
  concepts_lb_port  = 8002

  api_gateway_domain_name = "catalogue.api-${var.environment_name}.wellcomecollection.org"

  n_private_subnets        = length(var.private_subnets)
  min_task_count           = min(var.desired_task_counts.search, var.desired_task_counts.items, var.desired_task_counts.concepts)
  clamped_n_subnets        = min(local.n_private_subnets, local.min_task_count)
  routable_private_subnets = slice(var.private_subnets, 0, local.clamped_n_subnets)
}
