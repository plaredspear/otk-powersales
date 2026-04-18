###############################################################################
# ACM certificates
# - api_dev:  backend (ap-northeast-2, attached to ALB via EB)
# - web_dev:  frontend (us-east-1, attached to CloudFront — must be us-east-1)
#
# Validation CNAME records live in dns.tf.
###############################################################################

resource "aws_acm_certificate" "api_dev" {
  domain_name       = local.api_fqdn
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate" "web_dev" {
  provider = aws.us_east_1

  domain_name       = local.web_fqdn
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}
