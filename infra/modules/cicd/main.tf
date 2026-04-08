################################################################################
# GitHub OIDC Provider (for GitHub Actions → AWS authentication)
################################################################################

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["ffffffffffffffffffffffffffffffffffffffff"]

  tags = {
    Name = "${var.project}-${var.environment}-github-oidc"
  }
}

################################################################################
# CodeConnections — GitHub 연동 (CodeBuild → GitHub repo clone)
# NOTE: apply 후 AWS Console에서 1회 수동 승인 필요 (Step 6-1 참조)
################################################################################

resource "aws_codeconnections_connection" "github" {
  name          = "${var.project}-${var.environment}-github"
  provider_type = "GitHub"

  tags = {
    Environment = var.environment
  }
}

resource "aws_codebuild_source_credential" "github" {
  auth_type   = "CODECONNECTIONS"
  server_type = "GITHUB"
  token       = aws_codeconnections_connection.github.arn
}

################################################################################
# GitHub Actions Role (assumed via OIDC — triggers CodeBuild)
################################################################################

resource "aws_iam_role" "github_actions" {
  name = "${var.project}-${var.environment}-github-actions"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.github.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
          StringLike = {
            "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:ref:refs/heads/${var.github_deploy_branch}"
          }
        }
      }
    ]
  })

  tags = {
    Environment = var.environment
  }
}

resource "aws_iam_role_policy" "github_actions" {
  name = "${var.project}-${var.environment}-github-actions-policy"
  role = aws_iam_role.github_actions.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "codebuild:StartBuild",
          "codebuild:BatchGetBuilds"
        ]
        Resource = [
          aws_codebuild_project.backend.arn,
          aws_codebuild_project.web.arn,
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "logs:GetLogEvents"
        ]
        Resource = [
          "${aws_cloudwatch_log_group.codebuild_backend.arn}:*",
          "${aws_cloudwatch_log_group.codebuild_web.arn}:*",
        ]
      }
    ]
  })
}

################################################################################
# CodeBuild IAM Role — Backend
################################################################################

resource "aws_iam_role" "codebuild_backend" {
  name = "${var.project}-${var.environment}-codebuild-backend"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "codebuild.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Environment = var.environment
  }
}

resource "aws_iam_role_policy" "codebuild_backend" {
  name = "${var.project}-${var.environment}-codebuild-backend-policy"
  role = aws_iam_role.codebuild_backend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # CloudWatch Logs
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = [
          aws_cloudwatch_log_group.codebuild_backend.arn,
          "${aws_cloudwatch_log_group.codebuild_backend.arn}:*"
        ]
      },
      # ECR — login + push
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ]
        Resource = var.ecr_repository_arn
      },
      # ECS — deploy
      {
        Effect = "Allow"
        Action = [
          "ecs:UpdateService",
          "ecs:DescribeServices",
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition"
        ]
        Resource = "*"
      },
      # IAM — pass role for ECS task execution
      {
        Effect = "Allow"
        Action = [
          "iam:PassRole"
        ]
        Resource = var.ecs_task_role_arns
        Condition = {
          StringEquals = {
            "iam:PassedToService" = "ecs-tasks.amazonaws.com"
          }
        }
      },
      # CodeConnections — GitHub 소스 clone
      {
        Effect = "Allow"
        Action = [
          "codeconnections:GetConnectionToken",
          "codeconnections:GetConnection",
          "codeconnections:UseConnection"
        ]
        Resource = aws_codeconnections_connection.github.arn
      },
    ]
  })
}

################################################################################
# CodeBuild IAM Role — Web
################################################################################

resource "aws_iam_role" "codebuild_web" {
  name = "${var.project}-${var.environment}-codebuild-web"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "codebuild.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Environment = var.environment
  }
}

resource "aws_iam_role_policy" "codebuild_web" {
  name = "${var.project}-${var.environment}-codebuild-web-policy"
  role = aws_iam_role.codebuild_web.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # CloudWatch Logs
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = [
          aws_cloudwatch_log_group.codebuild_web.arn,
          "${aws_cloudwatch_log_group.codebuild_web.arn}:*"
        ]
      },
      # CodeConnections — GitHub 소스 clone
      {
        Effect = "Allow"
        Action = [
          "codeconnections:GetConnectionToken",
          "codeconnections:GetConnection",
          "codeconnections:UseConnection"
        ]
        Resource = aws_codeconnections_connection.github.arn
      },
      # S3 — Web Admin static files upload
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
          "s3:GetBucketLocation"
        ]
        Resource = var.admin_s3_bucket_arn != "" ? [
          var.admin_s3_bucket_arn,
          "${var.admin_s3_bucket_arn}/*"
        ] : []
      },
      # CloudFront — cache invalidation
      {
        Effect = "Allow"
        Action = [
          "cloudfront:CreateInvalidation"
        ]
        Resource = var.admin_cloudfront_distribution_arn != "" ? [var.admin_cloudfront_distribution_arn] : []
      }
    ]
  })
}

################################################################################
# CloudWatch Log Groups for CodeBuild
################################################################################

resource "aws_cloudwatch_log_group" "codebuild_backend" {
  name              = "/codebuild/${var.project}-${var.environment}-backend"
  retention_in_days = 14

  tags = {
    Environment = var.environment
  }
}

resource "aws_cloudwatch_log_group" "codebuild_web" {
  name              = "/codebuild/${var.project}-${var.environment}-web"
  retention_in_days = 14

  tags = {
    Environment = var.environment
  }
}

################################################################################
# CodeBuild Project — Backend
################################################################################

resource "aws_codebuild_project" "backend" {
  name          = "${var.project}-${var.environment}-deploy-backend"
  description   = "Build and deploy ${var.project} backend to ECS (${var.environment})"
  build_timeout = 15
  service_role  = aws_iam_role.codebuild_backend.arn

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/amazonlinux2-x86_64-standard:5.0"
    type                        = "LINUX_CONTAINER"
    privileged_mode             = true
    image_pull_credentials_type = "CODEBUILD"

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = var.aws_account_id
    }

    environment_variable {
      name  = "AWS_DEFAULT_REGION"
      value = var.region
    }

    environment_variable {
      name  = "ECR_REPOSITORY_URL"
      value = var.ecr_repository_url
    }

    environment_variable {
      name  = "ECS_CLUSTER_NAME"
      value = var.ecs_cluster_name
    }

    environment_variable {
      name  = "ECS_SERVICE_NAME"
      value = var.ecs_service_name
    }

    environment_variable {
      name  = "PROJECT"
      value = var.project
    }

    environment_variable {
      name  = "ENVIRONMENT"
      value = var.environment
    }
  }

  source {
    type            = "GITHUB"
    location        = var.github_repository_url
    git_clone_depth = 1
    buildspec       = file("${path.module}/buildspec-backend.yml")
  }

  cache {
    type  = "LOCAL"
    modes = ["LOCAL_DOCKER_LAYER_CACHE", "LOCAL_CUSTOM_CACHE"]
  }

  logs_config {
    cloudwatch_logs {
      group_name = aws_cloudwatch_log_group.codebuild_backend.name
    }
  }

  tags = {
    Environment = var.environment
  }
}

################################################################################
# CodeBuild Project — Web
################################################################################

resource "aws_codebuild_project" "web" {
  name          = "${var.project}-${var.environment}-deploy-web"
  description   = "Build and deploy ${var.project} web admin to S3/CloudFront (${var.environment})"
  build_timeout = 10
  service_role  = aws_iam_role.codebuild_web.arn

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/amazonlinux2-x86_64-standard:5.0"
    type                        = "LINUX_CONTAINER"
    privileged_mode             = false
    image_pull_credentials_type = "CODEBUILD"

    environment_variable {
      name  = "AWS_DEFAULT_REGION"
      value = var.region
    }

    environment_variable {
      name  = "ADMIN_S3_BUCKET_NAME"
      value = var.admin_s3_bucket_name
    }

    environment_variable {
      name  = "ADMIN_CLOUDFRONT_DISTRIBUTION_ID"
      value = var.admin_cloudfront_distribution_id
    }
  }

  source {
    type            = "GITHUB"
    location        = var.github_repository_url
    git_clone_depth = 1
    buildspec       = file("${path.module}/buildspec-web.yml")
  }

  cache {
    type  = "LOCAL"
    modes = ["LOCAL_CUSTOM_CACHE"]
  }

  logs_config {
    cloudwatch_logs {
      group_name = aws_cloudwatch_log_group.codebuild_web.name
    }
  }

  tags = {
    Environment = var.environment
  }
}
