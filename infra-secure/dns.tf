###############################################################################
# Route 53 records
# - api A-alias  → EB environment CNAME (regional hosted zone id)
# - web A-alias  → CloudFront distribution (global alias zone Z2FDTNDATAQYW2)
# - ACM DNS validation CNAMEs for api_dev and web_dev
###############################################################################

locals {
  # Elastic Beanstalk regional hosted zone id for ap-northeast-2
  eb_regional_zone_id = "Z3JE5OI70TWKCP"

  # Global CloudFront alias zone id (constant)
  cloudfront_alias_zone_id = "Z2FDTNDATAQYW2"
}

resource "aws_route53_record" "api_dev" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = local.api_fqdn
  type    = "A"

  alias {
    name                   = aws_elastic_beanstalk_environment.backend.cname
    zone_id                = local.eb_regional_zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "web_dev" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = local.web_fqdn
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.web.domain_name
    zone_id                = local.cloudfront_alias_zone_id
    evaluate_target_health = false
  }
}

# ---------------------------------------------------------------------------
# ACM DNS validation records — driven by the certificate's domain_validation_options
# ---------------------------------------------------------------------------
resource "aws_route53_record" "api_dev_validation" {
  for_each = {
    for dvo in aws_acm_certificate.api_dev.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id         = data.aws_route53_zone.root.zone_id
  name            = each.value.name
  type            = each.value.type
  records         = [each.value.record]
  ttl             = 300
  allow_overwrite = true
}

resource "aws_route53_record" "web_dev_validation" {
  for_each = {
    for dvo in aws_acm_certificate.web_dev.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id         = data.aws_route53_zone.root.zone_id
  name            = each.value.name
  type            = each.value.type
  records         = [each.value.record]
  ttl             = 300
  allow_overwrite = true
}
