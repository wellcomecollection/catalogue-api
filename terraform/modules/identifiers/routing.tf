locals {
  api_gateway_domain_name = "identifiers.api-${var.environment_name}.wellcomecollection.org"
}

data "aws_route53_zone" "dotorg" {
  provider = aws.dns
  name     = "wellcomecollection.org."
}

module "certificate" {
  source = "github.com/wellcomecollection/terraform-aws-acm-certificate?ref=v1.0.0"

  domain_name = local.api_gateway_domain_name

  zone_id = data.aws_route53_zone.dotorg.id

  providers = {
    aws.dns = aws.dns
  }
}

resource "aws_api_gateway_domain_name" "identifiers" {
  domain_name              = local.api_gateway_domain_name
  regional_certificate_arn = module.certificate.arn
  security_policy          = "TLS_1_2"

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_route53_record" "identifiers" {
  provider = aws.dns

  zone_id = data.aws_route53_zone.dotorg.id
  name    = aws_api_gateway_domain_name.identifiers.domain_name
  type    = "A"

  alias {
    name                   = aws_api_gateway_domain_name.identifiers.regional_domain_name
    zone_id                = aws_api_gateway_domain_name.identifiers.regional_zone_id
    evaluate_target_health = false
  }
}

# No base_path, so the hostname serves the spec's own paths unchanged.
resource "aws_api_gateway_base_path_mapping" "identifiers" {
  api_id      = aws_api_gateway_rest_api.identifiers.id
  stage_name  = aws_api_gateway_stage.default.stage_name
  domain_name = aws_api_gateway_domain_name.identifiers.domain_name
}
