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

resource "aws_api_gateway_domain_name" "catalogue_api" {
  domain_name              = local.api_gateway_domain_name
  regional_certificate_arn = module.certificate.arn
  security_policy          = "TLS_1_2"

  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_route53_record" "catalogue_api" {
  provider = aws.dns

  zone_id = data.aws_route53_zone.dotorg.id
  name    = aws_api_gateway_domain_name.catalogue_api.domain_name
  type    = "A"

  alias {
    name                   = aws_api_gateway_domain_name.catalogue_api.regional_domain_name
    zone_id                = aws_api_gateway_domain_name.catalogue_api.regional_zone_id
    evaluate_target_health = false
  }
}

resource "aws_api_gateway_base_path_mapping" "catalogue" {
  api_id      = aws_api_gateway_rest_api.catalogue.id
  stage_name  = "default"
  domain_name = aws_api_gateway_domain_name.catalogue_api.domain_name
  base_path   = "catalogue"
}
