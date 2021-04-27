resource "aws_acm_certificate" "catalogue_api" {
  domain_name               = local.api_gateway_domain_name
  validation_method         = "DNS"

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

resource "aws_apigatewayv2_domain_name" "catalogue_api" {
  domain_name              = local.api_gateway_domain_name

  domain_name_configuration {
    certificate_arn = aws_acm_certificate_validation.catalogue_api_validation.certificate_arn
    endpoint_type = "REGIONAL"
    security_policy = "TLS_1_2"
  }
}

locals {
  domain_config = aws_apigatewayv2_domain_name.catalogue_api.domain_name_configuration[0]
}

resource "aws_route53_record" "catalogue_api" {
  provider = aws.dns

  zone_id = data.aws_route53_zone.dotorg.id
  name    = aws_apigatewayv2_domain_name.catalogue_api.domain_name
  type    = "A"

  alias {
    name                   = local.domain_config["target_domain_name"]
    zone_id                = local.domain_config["hosted_zone_id"]
    evaluate_target_health = false
  }
}

resource "aws_apigatewayv2_api_mapping" "catalogue" {
  api_id      = aws_apigatewayv2_api.catalogue.id
  stage  = "$default"
  domain_name = aws_apigatewayv2_domain_name.catalogue_api.domain_name
  api_mapping_key = "catalogue"
}

resource "aws_apigatewayv2_deployment" "default" {
  api_id      = aws_apigatewayv2_api.catalogue.id

  triggers = {
    redeployment = filesha1("${path.module}/api_gateway.tf")
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.catalogue.id
  deployment_id = aws_apigatewayv2_deployment.default.id
  name = "$default"
}
