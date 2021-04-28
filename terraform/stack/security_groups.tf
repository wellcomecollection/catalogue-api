data "aws_vpc" "vpc" {
  id = var.vpc_id
}

resource "aws_security_group" "lb_ingress" {
  name        = "${var.environment_name}-catalogue_api-service_lb_ingress"
  description = "Allow traffic between services and the NLB"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [data.aws_vpc.vpc.cidr_block]
  }

  tags = {
    Name = "${var.environment_name}-lb-ingress"
  }
}

resource "aws_security_group" "egress" {
  name        = "${var.environment_name}-catalogue_api-egress"
  description = "Allows all egress traffic from the group"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.environment_name}-catalogue_api-egress"
  }
}

resource "aws_security_group" "interservice" {
  name        = "${var.environment_name}-catalogue_api-interservice"
  description = "Allow traffic between services"
  vpc_id      = var.vpc_id

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  tags = {
    Name = "${var.environment_name}-catalogue_api-interservice"
  }
}
