output "certificate_arn" {
  description = "ACM certificate ARN"
  value       = aws_acm_certificate.main.arn
}

output "certificate_validation_arn" {
  description = "ACM certificate validation ARN (use this for ALB listener)"
  value       = aws_acm_certificate_validation.main.certificate_arn
}
