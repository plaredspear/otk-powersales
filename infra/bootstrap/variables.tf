variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "state_bucket_name" {
  description = "S3 bucket name for Terraform state"
  type        = string
  default     = "otoki-terraform-state"
}

variable "project" {
  description = "Project name for tagging"
  type        = string
  default     = "otoki"
}
