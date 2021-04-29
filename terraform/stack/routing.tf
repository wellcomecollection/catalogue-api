resource "aws_acm_certificate" "catalogue_api" {
  domain_name       = local.api_gateway_domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

locals {
  validation_opts = tolist(aws_acm_certificate.catalogue_api.domain_validation_options)
}

data "aws_route53_zone" "dotorg" {
  provider = aws.dns
  name     = "wellcomecollection.org."
}

// Use count/lookup rather than for_each here because the newer syntax doesn't work
// https://github.com/hashicorp/terraform-provider-aws/issues/14447
resource "aws_route53_record" "cert_validation" {
  provider = aws.dns

  count   = length(local.validation_opts)
  name    = lookup(local.validation_opts[count.index], "resource_record_name")
  type    = lookup(local.validation_opts[count.index], "resource_record_type")
  zone_id = data.aws_route53_zone.dotorg.id
  records = [lookup(local.validation_opts[count.index], "resource_record_value")]
  ttl     = 60
}

resource "aws_acm_certificate_validation" "catalogue_api_validation" {
  certificate_arn         = aws_acm_certificate.catalogue_api.arn
  validation_record_fqdns = aws_route53_record.cert_validation.*.fqdn
}

resource "aws_api_gateway_domain_name" "catalogue_api" {
  domain_name              = local.api_gateway_domain_name
  regional_certificate_arn = aws_acm_certificate_validation.catalogue_api_validation.certificate_arn
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
