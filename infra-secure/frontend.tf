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
  api_origin_id  = "${local.api_fqdn}-alb"
  cloudfront_waf = "arn:aws:wafv2:us-east-1:${data.aws_caller_identity.current.account_id}:global/webacl/CreatedByCloudFront-244cfa60/efeb4336-7221-4cc1-a2e2-84dea587681a"

  # AWS-managed cache policy: CachingOptimized (정적 에셋)
  cache_policy_caching_optimized = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  # AWS-managed cache policy: CachingDisabled (/api/*)
  cache_policy_caching_disabled = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
  # AWS-managed origin request policy: AllViewer (Authorization/Cookie/Query 전체 전달)
  origin_request_policy_all_viewer = "216adef6-5c7f-47e4-b989-5492eafa07d3"
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
  # NOTE: price_class 미설정 — 본 distribution 은 CloudFront Free pricing plan
  # (월 1TB / 1천만 요청 무료) 에 등록되어 있어 price_class 지정이 금지된다.
  # API: "Distributions with the Free pricing plan can't have ... Price class"
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

  # ALB custom origin — /api/* 라우팅. api_fqdn 은 Route53 alias 를 통해 EB ALB 로
  # 해석되며, ALB 에 부착된 ACM 인증서(api_dev)의 CN 과 SNI 가 일치한다.
  origin {
    origin_id           = local.api_origin_id
    domain_name         = local.api_fqdn
    connection_attempts = 3
    connection_timeout  = 10

    custom_origin_config {
      http_port                = 80
      https_port               = 443
      origin_protocol_policy   = "https-only"
      origin_ssl_protocols     = ["TLSv1.2"]
      origin_read_timeout      = 30
      origin_keepalive_timeout = 5
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

  # /api/* — ALB 로 프록시. Same-origin 유지해 브라우저 CORS 회피. 응답은 캐시하지
  # 않고 Authorization/Cookie/Query 는 그대로 원본에 전달한다.
  ordered_cache_behavior {
    path_pattern             = "/api/*"
    target_origin_id         = local.api_origin_id
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    compress                 = true
    cache_policy_id          = local.cache_policy_caching_disabled
    origin_request_policy_id = local.origin_request_policy_all_viewer
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

  # NOTE: 이전에는 S3+OAC origin_access_identity "" vs null 드리프트 때문에
  # `ignore_changes = [origin]` 을 걸어두었으나, /api/* 라우팅을 위해 ALB origin
  # 을 명시 선언해야 하므로 제거. 드리프트가 재현되면 `origin_access_identity =
  # null` 로 맞추거나 origin 블록을 선별 ignore 하는 방식으로 재대응.
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
