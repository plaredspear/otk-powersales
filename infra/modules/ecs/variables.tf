variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
}

variable "target_group_arn" {
  description = "ALB target group ARN"
  type        = string
}

variable "ecr_repository_url" {
  description = "ECR repository URL"
  type        = string
}

variable "asg_arn" {
  description = "Auto Scaling Group ARN for ECS Capacity Provider"
  type        = string
}

variable "container_memory" {
  description = "Container hard memory limit in MiB"
  type        = number
  default     = 896
}

variable "container_memory_reservation" {
  description = "Container soft memory limit (memoryReservation) in MiB"
  type        = number
  default     = 512
}

variable "desired_count" {
  description = "Desired number of tasks"
  type        = number
  default     = 1
}

variable "db_host" {
  description = "Database host"
  type        = string
}

variable "db_port" {
  description = "Database port"
  type        = number
  default     = 5432
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "redis_host" {
  description = "Redis host"
  type        = string
}

variable "redis_port" {
  description = "Redis port"
  type        = number
  default     = 6379
}

variable "db_credentials_arn" {
  description = "Secrets Manager ARN for DB credentials"
  type        = string
}

variable "jwt_secret_arn" {
  description = "Secrets Manager ARN for JWT secret"
  type        = string
}

variable "secrets_arns" {
  description = "List of Secrets Manager ARNs for ECS execution role"
  type        = list(string)
}

variable "hikari_max_pool" {
  description = "HikariCP maximum pool size"
  type        = number
  default     = 5
}

variable "hikari_min_idle" {
  description = "HikariCP minimum idle connections"
  type        = number
  default     = 2
}

variable "log_level_root" {
  description = "Root log level"
  type        = string
  default     = "INFO"
}

variable "log_level_app" {
  description = "Application log level (com.otoki.internal)"
  type        = string
  default     = "DEBUG"
}

variable "log_level_security" {
  description = "Spring Security log level"
  type        = string
  default     = "INFO"
}

variable "orora_mock_enabled" {
  description = "Enable Orora API mock"
  type        = bool
  default     = true
}

variable "api_domain" {
  description = "API domain for Host header guard (e.g. dev-pwrs-api.codapt.kr)"
  type        = string
  default     = ""
}

variable "admin_domain" {
  description = "Admin domain for Host header guard (e.g. dev-pwrs-admin.codapt.kr)"
  type        = string
  default     = ""
}
