output "gitlab_ci_role_arn" {
  description = "IAM role ARN for GitLab CI OIDC authentication"
  value       = aws_iam_role.gitlab_ci.arn
}

output "codebuild_project_name" {
  description = "CodeBuild project name"
  value       = aws_codebuild_project.deploy.name
}

output "codebuild_project_arn" {
  description = "CodeBuild project ARN"
  value       = aws_codebuild_project.deploy.arn
}
