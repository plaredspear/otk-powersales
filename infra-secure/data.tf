data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

data "aws_route53_zone" "root" {
  name         = "${var.domain}."
  private_zone = false
}

data "aws_codestarconnections_connection" "github" {
  name = var.github_connection_name
}

data "aws_iam_role" "eb_service" {
  name = "aws-elasticbeanstalk-service-role"
}
