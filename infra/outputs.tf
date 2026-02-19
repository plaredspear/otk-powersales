output "vpc_id" {
  description = "VPC ID"
  value       = module.networking.vpc_id
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = module.ecr.repository_url
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.endpoint
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = module.alb.alb_dns_name
}

output "api_url" {
  description = "API URL"
  value       = "https://${var.domain_name}"
}

output "nat_gateway_public_ip" {
  description = "NAT Gateway Elastic IP — outbound IP for external system firewall whitelisting"
  value       = module.networking.nat_gateway_public_ip
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = module.ecs.service_name
}

output "gitlab_ci_role_arn" {
  description = "IAM role ARN for GitLab CI (set as DEV_AWS_ROLE_ARN / PROD_AWS_ROLE_ARN in GitLab CI/CD variables)"
  value       = module.cicd.gitlab_ci_role_arn
}

output "codebuild_project_name" {
  description = "CodeBuild project name (set as DEV_CODEBUILD_PROJECT / PROD_CODEBUILD_PROJECT in GitLab CI/CD variables)"
  value       = module.cicd.codebuild_project_name
}
