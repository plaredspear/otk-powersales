variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "domain_name" {
  description = "Domain name for the API (e.g., dev-api.company.com)"
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID"
  type        = string
}

variable "subject_alternative_names" {
  description = "ACM 인증서에 추가할 SAN 도메인 목록"
  type        = list(string)
  default     = []
}
