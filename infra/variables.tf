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
# ECS
################################################################################

variable "ecs_task_cpu" {
  description = "ECS task CPU units"
  type        = string
}

variable "ecs_task_memory" {
  description = "ECS task memory in MiB"
  type        = string
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

variable "gitlab_project_path" {
  description = "GitLab project path (e.g., group/project)"
  type        = string
}

variable "gitlab_deploy_branch" {
  description = "Branch that triggers deployment"
  type        = string
  default     = "main"
}
