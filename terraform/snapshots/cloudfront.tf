resource "aws_cloudfront_distribution" "data_api" {
  origin {
    domain_name = aws_s3_bucket.public_data.bucket_domain_name
    origin_id   = "data_api"

    custom_origin_config {
      https_port             = 443
      http_port              = 80
      origin_protocol_policy = "match-viewer"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  enabled         = true
  is_ipv6_enabled = true

  default_root_object = "index.html"

  aliases = [var.data_page_url]

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD", "OPTIONS"]
    target_origin_id = "data_api"

    forwarded_values {
      query_string = true

      cookies {
        forward = "none"
      }

      headers = [
        "Origin",
        "Access-Control-Request-Headers",
        "Access-Control-Request-Method"
      ]
    }

    viewer_protocol_policy = "redirect-to-https"

    min_ttl     = 7200
    default_ttl = 86400
    max_ttl     = 86400
  }

  price_class = "PriceClass_100"

  viewer_certificate {
    acm_certificate_arn      = module.certificate.arn
    minimum_protocol_version = "TLSv1"
    ssl_support_method       = "sni-only"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
}
