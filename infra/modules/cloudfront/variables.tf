variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "admin_domain_name" {
  description = "Admin domain name (e.g., dev-pwrs-admin.codapt.kr)"
  type        = string
}

variable "api_domain_name" {
  description = "API domain name — used as ALB Origin domain"
  type        = string
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN in us-east-1 for CloudFront"
  type        = string
}
