data "aws_route53_zone" "dotorg" {
  provider = aws.dns

  name = "wellcomecollection.org."
}

module "certificate" {
  source = "github.com/wellcomecollection/terraform-aws-acm-certificate?ref=v1.0.0"

  domain_name = var.data_page_url

  zone_id = data.aws_route53_zone.dotorg.id

  providers = {
    aws     = aws.us_east_1
    aws.dns = aws.dns
  }
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
