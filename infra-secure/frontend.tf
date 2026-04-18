###############################################################################
# Frontend — CloudFront distribution fronting the web S3 bucket via OAC
#
# Viewer cert:  ACM in us-east-1 (required by CloudFront)
# WAF:         pre-existing WebACL auto-created by CloudFront "one-click"
#              protection; kept as literal ARN (not managed here).
# SPA fallback: 403/404 → /index.html (200) so client-side routes work.
###############################################################################

locals {
  web_origin_id  = "dev-otk-pwrs-web.s3.ap-northeast-2.amazonaws.com-mo149b51345"
  cloudfront_waf = "arn:aws:wafv2:us-east-1:${data.aws_caller_identity.current.account_id}:global/webacl/CreatedByCloudFront-244cfa60/efeb4336-7221-4cc1-a2e2-84dea587681a"

  # AWS-managed cache policy: CachingOptimized
  cache_policy_caching_optimized = "658327ea-f89d-4fab-a63d-7e88639e58f6"
}

resource "aws_cloudfront_origin_access_control" "web" {
  name                              = "oac-dev-otk-pwrs-web.s3.ap-northeast-2.amazonaws.com-mo14hf3nj8s"
  description                       = "Created by CloudFront"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "web" {
  enabled             = true
  is_ipv6_enabled     = true
  http_version        = "http2"
  price_class         = "PriceClass_All"
  aliases             = [local.web_fqdn]
  web_acl_id          = local.cloudfront_waf
  retain_on_delete    = false
  wait_for_deployment = false

  origin {
    origin_id                = local.web_origin_id
    domain_name              = aws_s3_bucket.web.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.web.id
    connection_attempts      = 3
    connection_timeout       = 10

    s3_origin_config {
      origin_access_identity = ""
    }
  }

  default_cache_behavior {
    target_origin_id       = local.web_origin_id
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["HEAD", "GET"]
    cached_methods         = ["HEAD", "GET"]
    compress               = true
    cache_policy_id        = local.cache_policy_caching_optimized
  }

  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate.web_dev.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # Known provider drift: aws_cloudfront_distribution with S3+OAC oscillates on
  # origin.s3_origin_config.origin_access_identity ("" vs null). Ignore the
  # origin block to stabilize plan diff 0; update origin via a targeted change
  # if/when needed.
  lifecycle {
    ignore_changes = [origin]
  }
}

###############################################################################
# S3 bucket policy — allow CloudFront (via OAC) to read from the web bucket
###############################################################################
data "aws_iam_policy_document" "web_bucket" {
  statement {
    sid       = "AllowCloudFrontOAC"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.web.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.web.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "web" {
  bucket = aws_s3_bucket.web.id
  policy = data.aws_iam_policy_document.web_bucket.json
}
