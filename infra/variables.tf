################################################################################
# General
################################################################################

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

variable "aws_account_id" {
  description = "AWS account ID"
  type        = string
}

################################################################################
# Networking
################################################################################

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
}

variable "public_subnet_cidrs" {
  description = "Public subnet CIDR blocks"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "Private subnet CIDR blocks"
  type        = list(string)
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
}

################################################################################
# RDS
################################################################################

variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "rds_engine_version" {
  description = "PostgreSQL engine version"
  type        = string
}

variable "rds_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ for RDS"
  type        = bool
}

variable "rds_publicly_accessible" {
  description = "RDS 퍼블릭 접근 허용 (dev 전용)"
  type        = bool
  default     = false
}

variable "rds_allowed_cidrs" {
  description = "RDS 외부 접근 허용 CIDR 목록"
  type        = list(string)
  default     = []
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_username" {
  description = "Database master username"
  type        = string
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}

################################################################################
# ElastiCache
################################################################################

variable "elasticache_node_type" {
  description = "ElastiCache node type"
  type        = string
}

################################################################################
# EC2 (ECS EC2 launch type)
################################################################################

variable "ec2_instance_type" {
  description = "EC2 instance type for ECS"
  type        = string
  default     = "t3.small"
}

variable "ec2_asg_min_size" {
  description = "ASG minimum instance count"
  type        = number
  default     = 1
}

variable "ec2_asg_max_size" {
  description = "ASG maximum instance count"
  type        = number
  default     = 3
}

variable "ec2_asg_desired_capacity" {
  description = "ASG desired instance count"
  type        = number
  default     = 1
}

################################################################################
# ECS
################################################################################

variable "ecs_container_memory" {
  description = "Container hard memory limit in MiB"
  type        = number
  default     = 896
}

variable "ecs_container_memory_reservation" {
  description = "Container soft memory limit (memoryReservation) in MiB"
  type        = number
  default     = 512
}

variable "ecs_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
}

################################################################################
# DNS
################################################################################

variable "domain_name" {
  description = "Domain name for the API"
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID"
  type        = string
}

################################################################################
# Secrets
################################################################################

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

################################################################################
# CI/CD
################################################################################

variable "github_repo" {
  description = "GitHub repository (e.g., owner/repo)"
  type        = string
}

variable "github_repository_url" {
  description = "GitHub repository HTTPS URL (e.g., https://github.com/owner/repo.git)"
  type        = string
}

variable "github_deploy_branch" {
  description = "Branch that triggers deployment"
  type        = string
  default     = "main"
}
