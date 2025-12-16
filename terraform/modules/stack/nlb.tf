resource "aws_lb" "catalogue_api" {
  name               = "${var.environment_name}-catalogue-api"
  internal           = true
  load_balancer_type = "network"
  subnets            = local.routable_private_subnets

  enable_cross_zone_load_balancing = true
}
