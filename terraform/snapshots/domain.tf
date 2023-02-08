data "aws_route53_zone" "dotorg" {
  provider = aws.dns

  name = "wellcomecollection.org."
}

resource "aws_acm_certificate" "data_page" {
  provider = aws.us_east_1

  domain_name       = var.data_page_url
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

moved {
  from = aws_route53_record.cert_validation
  to   = aws_route53_record.cert_validation["data.wellcomecollection.org"]
}

resource "aws_route53_record" "cert_validation" {
  provider = aws.dns
  for_each = {
    for dvo in aws_acm_certificate.data_page.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  name    = each.value.name
  type    = each.value.type
  zone_id = data.aws_route53_zone.dotorg.id
  records = [each.value.record]
  ttl     = 60
}

resource "aws_acm_certificate_validation" "catalogue_api_validation" {
  provider = aws.us_east_1

  certificate_arn         = aws_acm_certificate.data_page.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

resource "aws_route53_record" "data_page" {
  provider = aws.dns

  zone_id = data.aws_route53_zone.dotorg.id
  name    = var.data_page_url
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.data_api.domain_name
    zone_id                = aws_cloudfront_distribution.data_api.hosted_zone_id
    evaluate_target_health = false
  }
}
