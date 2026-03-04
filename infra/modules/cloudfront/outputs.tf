output "distribution_id" {
  description = "CloudFront Distribution ID"
  value       = aws_cloudfront_distribution.admin.id
}

output "distribution_domain_name" {
  description = "CloudFront domain name (xxx.cloudfront.net)"
  value       = aws_cloudfront_distribution.admin.domain_name
}

output "distribution_hosted_zone_id" {
  description = "CloudFront hosted zone ID for Route53 alias"
  value       = aws_cloudfront_distribution.admin.hosted_zone_id
}

output "s3_bucket_name" {
  description = "S3 bucket name"
  value       = aws_s3_bucket.admin_web.id
}

output "s3_bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.admin_web.arn
}
