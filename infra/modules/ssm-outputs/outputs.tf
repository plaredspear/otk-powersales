output "parameter_arns" {
  description = "ARNs of created SSM parameters (for IAM policies)"
  value       = [for p in aws_ssm_parameter.output : p.arn]
}
