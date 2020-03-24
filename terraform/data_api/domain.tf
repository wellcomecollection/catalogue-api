/*data "aws_route53_zone" "dotorg" {
  provider = aws.routemaster

  name = "wellcomecollection.org."
}*/

locals {
  # This is the Zone ID for wellcomecollection.org in the routemaster account.
  # We can't look this up programatically because the role we use doesn't have
  # the right permissions in that account.
  route53_zone_id = "Z3THRVQ5VDYDMC"
}

resource "aws_acm_certificate" "data_page" {
  provider = aws.us_east_1

  domain_name       = var.data_page_url
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "cert_validation" {
  provider = aws.routemaster

  name    = aws_acm_certificate.data_page.domain_validation_options.0.resource_record_name
  type    = aws_acm_certificate.data_page.domain_validation_options.0.resource_record_type
  zone_id = local.route53_zone_id
  records = [aws_acm_certificate.data_page.domain_validation_options.0.resource_record_value]
  ttl     = 60
}

resource "aws_acm_certificate_validation" "catalogue_api_validation" {
  provider = aws.us_east_1

  certificate_arn         = aws_acm_certificate.data_page.arn
  validation_record_fqdns = [aws_route53_record.cert_validation.fqdn]
}

resource "aws_route53_record" "data_page" {
  provider = aws.routemaster

  zone_id = local.route53_zone_id
  name    = var.data_page_url
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.data_api.domain_name
    zone_id                = aws_cloudfront_distribution.data_api.hosted_zone_id
    evaluate_target_health = false
  }
}
