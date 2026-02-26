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

variable "github_repo" {
  description = "GitHub repository (e.g., owner/repo)"
  type        = string
}

variable "github_deploy_branch" {
  description = "Branch that triggers deployment (e.g., main, develop)"
  type        = string
  default     = "main"
}

variable "github_repository_url" {
  description = "GitHub repository HTTPS URL (e.g., https://github.com/owner/repo.git)"
  type        = string
}


variable "ecr_repository_url" {
  description = "ECR repository URL"
  type        = string
}

variable "ecr_repository_arn" {
  description = "ECR repository ARN"
  type        = string
}

variable "ecs_cluster_name" {
  description = "ECS cluster name"
  type        = string
}

variable "ecs_service_name" {
  description = "ECS service name"
  type        = string
}

variable "ecs_task_role_arns" {
  description = "List of ECS task/execution role ARNs for iam:PassRole"
  type        = list(string)
}
