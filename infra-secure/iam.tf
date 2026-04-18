###############################################################################
# IAM — roles and policies for EB EC2, CodeBuild (backend/web), CodePipeline (backend/web)
#
# ARN references:
# - S3 buckets / ECR → Terraform resource references (strong typing)
# - CloudFront distribution / CodeBuild projects → hardcoded in wildcard form
#   because the policies already exist in console with resource-scoped ARNs and
#   cycle back to resources defined later in this root module. Switching to
#   resource refs after all categories are imported is a trivial follow-up.
# - EB default service bucket (`elasticbeanstalk-ap-northeast-2-<account>`) →
#   auto-managed by Elastic Beanstalk, referenced by ARN (not Terraform-managed).
# - SNS topic `dev-otk-pwrs-alerts` → not currently deployed; policy ARN is
#   retained for forward compatibility (publishing is harmless if topic absent).
###############################################################################

locals {
  eb_platform_bucket_arn = "arn:aws:s3:::elasticbeanstalk-${data.aws_region.current.name}-${data.aws_caller_identity.current.account_id}"
  codebuild_backend_arn  = "arn:aws:codebuild:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:project/${local.name_prefix}-backend-build"
  codebuild_web_arn      = "arn:aws:codebuild:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:project/${local.name_prefix}-web-build"
  alerts_sns_arn         = "arn:aws:sns:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:${local.name_prefix}-alerts"
  codebuild_logs_arn     = "arn:aws:logs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:log-group:/aws/codebuild/*"
  # CloudFront distribution ARN (hardcoded; see frontend.tf for the actual resource).
  cloudfront_web_arn = "arn:aws:cloudfront::${data.aws_caller_identity.current.account_id}:distribution/E39VFE7PXUO1S"
}

# ---------------------------------------------------------------------------
# Trust policy documents
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "ec2_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "codebuild_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codebuild.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "codepipeline_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codepipeline.amazonaws.com"]
    }
  }
}

###############################################################################
# EB EC2 instance role (+ instance profile)
###############################################################################

resource "aws_iam_role" "eb_ec2" {
  name               = "${local.name_prefix}-eb-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "eb_ec2_web_tier" {
  role       = aws_iam_role.eb_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier"
}
resource "aws_iam_role_policy_attachment" "eb_ec2_ecr_read" {
  role       = aws_iam_role.eb_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}
resource "aws_iam_role_policy_attachment" "eb_ec2_cw_agent" {
  role       = aws_iam_role.eb_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}
resource "aws_iam_role_policy_attachment" "eb_ec2_ssm" {
  role       = aws_iam_role.eb_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "eb_ec2_storage" {
  name = "${local.name_prefix}-eb-ec2-storage-policy"
  role = aws_iam_role.eb_ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Storage"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ]
        Resource = [
          aws_s3_bucket.storage.arn,
          "${aws_s3_bucket.storage.arn}/*",
        ]
      },
    ]
  })
}

# ---------------------------------------------------------------------------
# App config read — Parameter Store (non-secret) + RDS master user Secrets
# Manager secret (password). KMS:Decrypt 는 master_user_secret 전용 키를 대상으
# 로 하고, Parameter Store 는 String 타입이라 별도 kms:Decrypt 불필요.
# ---------------------------------------------------------------------------
resource "aws_iam_role_policy" "eb_ec2_app_config" {
  name = "${local.name_prefix}-eb-ec2-app-config-policy"
  role = aws_iam_role.eb_ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ParameterStoreRead"
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath",
        ]
        Resource = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter${local.param_prefix}/*"
      },
      {
        Sid    = "RDSSecretRead"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = aws_db_instance.main.master_user_secret[0].secret_arn
      },
      {
        Sid      = "KmsDecryptRDSSecret"
        Effect   = "Allow"
        Action   = "kms:Decrypt"
        Resource = aws_db_instance.main.master_user_secret[0].kms_key_id
      },
    ]
  })
}

resource "aws_iam_instance_profile" "eb_ec2" {
  name = "${local.name_prefix}-eb-ec2-role"
  role = aws_iam_role.eb_ec2.name
}

###############################################################################
# Backend CodeBuild role
###############################################################################

resource "aws_iam_role" "backend_build" {
  name               = "${local.name_prefix}-backend-build-role"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
}

resource "aws_iam_role_policy" "backend_build" {
  name = "${local.name_prefix}-backend-build-policy"
  role = aws_iam_role.backend_build.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "ECRAuth"
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        Sid    = "ECRPush"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
        ]
        Resource = aws_ecr_repository.backend.arn
      },
      {
        Sid    = "PipelineArtifacts"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:GetBucketAcl",
          "s3:GetBucketLocation",
        ]
        Resource = [
          aws_s3_bucket.pipeline_artifacts.arn,
          "${aws_s3_bucket.pipeline_artifacts.arn}/*",
        ]
      },
      {
        Sid    = "EBBucket"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:GetBucketLocation",
        ]
        Resource = [
          local.eb_platform_bucket_arn,
          "${local.eb_platform_bucket_arn}/*",
        ]
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = local.codebuild_logs_arn
      },
      {
        Sid    = "CodeConnections"
        Effect = "Allow"
        Action = [
          "codeconnections:UseConnection",
          "codeconnections:GetConnection",
          "codeconnections:GetConnectionToken",
          "codestar-connections:UseConnection",
          "codestar-connections:GetConnection",
          "codestar-connections:GetConnectionToken",
        ]
        Resource = "arn:aws:codeconnections:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:connection/*"
      },
      {
        Sid    = "AppConfigRead"
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath",
        ]
        Resource = "arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter${local.param_prefix}/*"
      },
    ]
  })
}

###############################################################################
# Web CodeBuild role
###############################################################################

resource "aws_iam_role" "web_build" {
  name               = "${local.name_prefix}-web-build-role"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume.json
}

resource "aws_iam_role_policy" "web_build" {
  name = "${local.name_prefix}-web-build-policy"
  role = aws_iam_role.web_build.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Deploy"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ]
        Resource = [
          aws_s3_bucket.web.arn,
          "${aws_s3_bucket.web.arn}/*",
        ]
      },
      {
        Sid    = "PipelineArtifacts"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:GetBucketAcl",
          "s3:GetBucketLocation",
        ]
        Resource = [
          aws_s3_bucket.pipeline_artifacts.arn,
          "${aws_s3_bucket.pipeline_artifacts.arn}/*",
        ]
      },
      {
        Sid      = "CloudFrontInvalidation"
        Effect   = "Allow"
        Action   = "cloudfront:CreateInvalidation"
        Resource = local.cloudfront_web_arn
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = local.codebuild_logs_arn
      },
    ]
  })
}

###############################################################################
# Backend CodePipeline role
###############################################################################

resource "aws_iam_role" "pipeline" {
  name               = "${local.name_prefix}-pipeline-role"
  assume_role_policy = data.aws_iam_policy_document.codepipeline_assume.json
}

resource "aws_iam_role_policy_attachment" "pipeline_eb_admin" {
  role       = aws_iam_role.pipeline.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess-AWSElasticBeanstalk"
}

resource "aws_iam_role_policy" "pipeline" {
  name = "${local.name_prefix}-pipeline-policy"
  role = aws_iam_role.pipeline.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "GitHubConnection"
        Effect = "Allow"
        Action = [
          "codeconnections:UseConnection",
          "codeconnections:GetConnection",
          "codeconnections:GetConnectionToken",
          "codestar-connections:UseConnection",
          "codestar-connections:GetConnection",
          "codestar-connections:GetConnectionToken",
        ]
        Resource = [
          "arn:aws:codeconnections:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:*",
          "arn:aws:codestar-connections:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:*",
        ]
      },
      {
        Sid    = "PipelineArtifacts"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:GetObjectAcl",
          "s3:PutObject",
          "s3:GetBucketAcl",
          "s3:GetBucketLocation",
          "s3:GetBucketVersioning",
          "s3:ListBucket",
        ]
        Resource = [
          aws_s3_bucket.pipeline_artifacts.arn,
          "${aws_s3_bucket.pipeline_artifacts.arn}/*",
        ]
      },
      {
        Sid    = "CodeBuildStart"
        Effect = "Allow"
        Action = [
          "codebuild:BatchGetBuilds",
          "codebuild:StartBuild",
        ]
        Resource = local.codebuild_backend_arn
      },
      {
        Sid      = "SNSApproval"
        Effect   = "Allow"
        Action   = "sns:Publish"
        Resource = local.alerts_sns_arn
      },
    ]
  })
}

###############################################################################
# Web CodePipeline role
###############################################################################

resource "aws_iam_role" "web_pipeline" {
  name               = "${local.name_prefix}-web-pipeline-role"
  assume_role_policy = data.aws_iam_policy_document.codepipeline_assume.json
}

resource "aws_iam_role_policy" "web_pipeline" {
  name = "${local.name_prefix}-web-pipeline-policy"
  role = aws_iam_role.web_pipeline.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "GitHubConnection"
        Effect = "Allow"
        Action = [
          "codeconnections:UseConnection",
          "codeconnections:GetConnection",
          "codeconnections:GetConnectionToken",
          "codestar-connections:UseConnection",
          "codestar-connections:GetConnection",
          "codestar-connections:GetConnectionToken",
        ]
        Resource = [
          "arn:aws:codeconnections:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:connection/*",
          "arn:aws:codestar-connections:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:connection/*",
        ]
      },
      {
        Sid    = "PipelineArtifacts"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:GetBucketAcl",
          "s3:GetBucketLocation",
        ]
        Resource = [
          aws_s3_bucket.pipeline_artifacts.arn,
          "${aws_s3_bucket.pipeline_artifacts.arn}/*",
        ]
      },
      {
        Sid    = "CodeBuildStart"
        Effect = "Allow"
        Action = [
          "codebuild:BatchGetBuilds",
          "codebuild:StartBuild",
        ]
        Resource = local.codebuild_web_arn
      },
      {
        Sid      = "SNSApproval"
        Effect   = "Allow"
        Action   = "sns:Publish"
        Resource = local.alerts_sns_arn
      },
    ]
  })
}
