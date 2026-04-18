variable "aws_profile" {
  description = "AWS CLI named profile to use"
  type        = string
  default     = "dev-codapt"
}

variable "region" {
  description = "Primary AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project" {
  description = "Project short name used in resource naming"
  type        = string
  default     = "otk-pwrs"
}

variable "stage" {
  description = "Deployment stage (dev/prod)"
  type        = string
  default     = "dev"
}

variable "domain" {
  description = "Root DNS domain managed in Route 53 (not managed by Terraform, referenced as data source)"
  type        = string
  default     = "codapt.kr"
}

variable "api_subdomain" {
  description = "API subdomain (joined with domain for backend DNS/ACM)"
  type        = string
  default     = "dev-powersalesapi"
}

variable "web_subdomain" {
  description = "Web subdomain (joined with domain for frontend DNS/ACM)"
  type        = string
  default     = "dev-powersales"
}

variable "github_repository" {
  description = "GitHub FullRepositoryId (owner/repo) used by CodePipeline source action"
  type        = string
  default     = "plaredspear/dev-otk-pwrs"
}

variable "github_connection_name" {
  description = "CodeConnections connection name (pre-existing, referenced as data source)"
  type        = string
  default     = "dev-otk-pwrs"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "rds_master_username" {
  description = "RDS master username (password is auto-generated and stored in Secrets Manager via manage_master_user_password)"
  type        = string
  default     = "otkadmin"
}

# ---------------------------------------------------------------------------
# CloudFront/OAC/ACM(us-east-1) 재사용
#
# CloudFront 는 생성된 당월 billing cycle 종료 전까지 삭제 불가 (Free pricing
# plan 자동 가입). 따라서 `terraform destroy` 시에는 CloudFront/OAC/ACM(web_dev)
# 을 `terraform state rm` 으로 state 에서 제외하고 AWS 에 남긴 뒤,
# 재구축 apply 때 이 변수로 기존 ID/ARN 을 지정하면 자동 import 된다.
# 신규 구축(잔존 리소스 없음) 시에는 전부 null 유지 → import 블록 no-op.
# ---------------------------------------------------------------------------
variable "reuse_web_stack" {
  description = "재구축 시 흡수할 기존 CloudFront/OAC/ACM(web_dev) ID·ARN. 각 항목이 null 이면 해당 리소스는 import 없이 신규 생성."
  type = object({
    distribution_id = optional(string)
    oac_id          = optional(string)
    acm_cert_arn    = optional(string)
  })
  default = {}
}
