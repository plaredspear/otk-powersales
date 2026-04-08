output "github_actions_role_arn" {
  description = "IAM role ARN for GitHub Actions OIDC authentication"
  value       = aws_iam_role.github_actions.arn
}

output "codebuild_backend_project_name" {
  description = "CodeBuild backend project name"
  value       = aws_codebuild_project.backend.name
}

output "codebuild_backend_project_arn" {
  description = "CodeBuild backend project ARN"
  value       = aws_codebuild_project.backend.arn
}

output "codebuild_web_project_name" {
  description = "CodeBuild web project name"
  value       = aws_codebuild_project.web.name
}

output "codebuild_web_project_arn" {
  description = "CodeBuild web project ARN"
  value       = aws_codebuild_project.web.arn
}
