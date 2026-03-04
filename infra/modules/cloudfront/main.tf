################################################################################
# S3 Bucket — Web Admin static files
################################################################################

resource "aws_s3_bucket" "admin_web" {
  bucket        = "${var.project}-${var.environment}-admin-web"
  force_destroy = var.environment == "dev"

  tags = {
    Name        = "${var.project}-${var.environment}-admin-web"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_public_access_block" "admin_web" {
  bucket = aws_s3_bucket.admin_web.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_policy" "admin_web" {
  bucket = aws_s3_bucket.admin_web.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.admin_web.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.admin.arn
          }
        }
      }
    ]
  })
}

################################################################################
# CloudFront OAC (Origin Access Control)
################################################################################

resource "aws_cloudfront_origin_access_control" "admin" {
  name                              = "${var.project}-${var.environment}-admin-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

################################################################################
# CloudFront Distribution
################################################################################

resource "aws_cloudfront_distribution" "admin" {
  aliases             = [var.admin_domain_name]
  default_root_object = "index.html"
  enabled             = true
  price_class         = "PriceClass_200"
  http_version        = "http2and3"

  # S3 Origin — static files
  origin {
    origin_id                = "s3-admin-web"
    domain_name              = aws_s3_bucket.admin_web.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.admin.id
  }

  # ALB Origin — API pass-through
  origin {
    origin_id   = "alb-backend"
    domain_name = var.api_domain_name

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # Default behavior — S3 (static files)
  default_cache_behavior {
    target_origin_id       = "s3-admin-web"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6" # CachingOptimized
    compress               = true
  }

  # /api/* — ALB (API pass-through)
  ordered_cache_behavior {
    path_pattern             = "/api/*"
    target_origin_id         = "alb-backend"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad" # CachingDisabled
    origin_request_policy_id = "b689b0a8-53d0-40ab-baf2-68738e2966ac" # AllViewerExceptHostHeader
  }

  # SPA fallback — 403/404 → /index.html
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
    acm_certificate_arn      = var.acm_certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = {
    Name        = "${var.project}-${var.environment}-admin-cf"
    Environment = var.environment
  }
}
