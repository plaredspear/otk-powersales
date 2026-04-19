###############################################################################
# CI/CD — CodeBuild projects + CodePipeline V2 pipelines (backend, web)
#
# Triggers: V2 push triggers with file-path filters so each pipeline only
# fires when its own subtree changes (backend/** or web/**).
# Source: GitHub via CodeConnections (connection managed outside Terraform).
###############################################################################

# ---------------------------------------------------------------------------
# CodeBuild — backend
# ---------------------------------------------------------------------------
resource "aws_codebuild_project" "backend" {
  name          = "${local.name_prefix}-backend-build"
  service_role  = aws_iam_role.backend_build.arn
  build_timeout = 60

  artifacts {
    type = "NO_ARTIFACTS"
  }

  # privileged_mode=true: buildspec 의 `docker build` / `docker push` 가 Docker 데몬 접근을 요구.
  environment {
    type                        = "LINUX_CONTAINER"
    image                       = "aws/codebuild/amazonlinux-x86_64-standard:6.0"
    compute_type                = "BUILD_GENERAL1_MEDIUM"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode             = true

    environment_variable {
      name  = "AWS_DEFAULT_REGION"
      value = data.aws_region.current.name
    }

    environment_variable {
      name  = "IMAGE_REPO_NAME"
      value = aws_ecr_repository.backend.name
    }

    environment_variable {
      name  = "STAGE"
      value = var.stage
    }

    environment_variable {
      name  = "PROJECT"
      value = var.project
    }

    environment_variable {
      name  = "PARAM_PREFIX"
      value = local.param_prefix
    }
  }

  source {
    type                = "GITHUB"
    location            = "https://github.com/${var.github_repository}"
    git_clone_depth     = 1
    buildspec           = "backend/buildspec.yml"
    report_build_status = false
    insecure_ssl        = false

    git_submodules_config {
      fetch_submodules = false
    }
  }

  logs_config {
    cloudwatch_logs {
      status = "ENABLED"
    }
    s3_logs {
      status              = "DISABLED"
      encryption_disabled = false
    }
  }
}

# ---------------------------------------------------------------------------
# CodeBuild — web
# ---------------------------------------------------------------------------
resource "aws_codebuild_project" "web" {
  name          = "${local.name_prefix}-web-build"
  service_role  = aws_iam_role.web_build.arn
  build_timeout = 60

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    type                        = "LINUX_CONTAINER"
    image                       = "aws/codebuild/amazonlinux-x86_64-standard:6.0"
    compute_type                = "BUILD_GENERAL1_SMALL"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode             = false

    environment_variable {
      name  = "S3_BUCKET"
      value = aws_s3_bucket.web.id
    }

    environment_variable {
      name  = "DISTRIBUTION_ID"
      value = aws_cloudfront_distribution.web.id
    }
  }

  source {
    type                = "GITHUB"
    location            = "https://github.com/${var.github_repository}"
    git_clone_depth     = 1
    buildspec           = "web/buildspec.yml"
    report_build_status = false
    insecure_ssl        = false

    git_submodules_config {
      fetch_submodules = false
    }
  }

  logs_config {
    cloudwatch_logs {
      status = "ENABLED"
    }
    s3_logs {
      status              = "DISABLED"
      encryption_disabled = false
    }
  }
}

# ---------------------------------------------------------------------------
# CodePipeline — backend (Source → Build → Deploy to EB)
# ---------------------------------------------------------------------------
resource "aws_codepipeline" "backend" {
  name           = "${local.name_prefix}-backend-pipeline"
  role_arn       = aws_iam_role.pipeline.arn
  pipeline_type  = "V2"
  execution_mode = "QUEUED"

  artifact_store {
    location = aws_s3_bucket.pipeline_artifacts.id
    type     = "S3"
  }

  stage {
    name = "Source"

    on_failure {
      result = "RETRY"

      retry_configuration {
        retry_mode = "ALL_ACTIONS"
      }
    }

    action {
      name             = "Source"
      category         = "Source"
      owner            = "AWS"
      provider         = "CodeStarSourceConnection"
      version          = "1"
      run_order        = 1
      namespace        = "SourceVariables"
      output_artifacts = ["SourceArtifact"]
      region           = data.aws_region.current.name

      configuration = {
        BranchName           = "dev"
        ConnectionArn        = data.aws_codestarconnections_connection.github.arn
        DetectChanges        = "false"
        FullRepositoryId     = var.github_repository
        OutputArtifactFormat = "CODE_ZIP"
      }
    }
  }

  stage {
    name = "Build"

    on_failure {
      result = "RETRY"

      retry_configuration {
        retry_mode = "ALL_ACTIONS"
      }
    }

    action {
      name             = "Build"
      category         = "Build"
      owner            = "AWS"
      provider         = "CodeBuild"
      version          = "1"
      run_order        = 1
      namespace        = "BuildVariables"
      input_artifacts  = ["SourceArtifact"]
      output_artifacts = ["BuildArtifact"]
      region           = data.aws_region.current.name

      configuration = {
        ProjectName = aws_codebuild_project.backend.name
      }
    }
  }

  stage {
    name = "Deploy"

    on_failure {
      result = "ROLLBACK"
    }

    action {
      name            = "Deploy"
      category        = "Deploy"
      owner           = "AWS"
      provider        = "ElasticBeanstalk"
      version         = "1"
      run_order       = 1
      namespace       = "DeployVariables"
      input_artifacts = ["BuildArtifact"]
      region          = data.aws_region.current.name

      configuration = {
        ApplicationName = aws_elastic_beanstalk_application.backend.name
        EnvironmentName = aws_elastic_beanstalk_environment.backend.name
      }
    }
  }

  trigger {
    provider_type = "CodeStarSourceConnection"

    git_configuration {
      source_action_name = "Source"

      push {
        branches {
          includes = ["dev"]
        }
        file_paths {
          includes = ["backend/**"]
        }
      }
    }
  }
}

# ---------------------------------------------------------------------------
# CodePipeline — web (Source → Build; web build does its own S3 deploy)
# ---------------------------------------------------------------------------
resource "aws_codepipeline" "web" {
  name           = "${local.name_prefix}-web-pipeline"
  role_arn       = aws_iam_role.web_pipeline.arn
  pipeline_type  = "V2"
  execution_mode = "QUEUED"

  artifact_store {
    location = aws_s3_bucket.pipeline_artifacts.id
    type     = "S3"
  }

  stage {
    name = "Source"

    on_failure {
      result = "RETRY"

      retry_configuration {
        retry_mode = "ALL_ACTIONS"
      }
    }

    action {
      name             = "Source"
      category         = "Source"
      owner            = "AWS"
      provider         = "CodeStarSourceConnection"
      version          = "1"
      run_order        = 1
      namespace        = "SourceVariables"
      output_artifacts = ["SourceArtifact"]
      region           = data.aws_region.current.name

      configuration = {
        BranchName           = "dev"
        ConnectionArn        = data.aws_codestarconnections_connection.github.arn
        DetectChanges        = "false"
        FullRepositoryId     = var.github_repository
        OutputArtifactFormat = "CODE_ZIP"
      }
    }
  }

  stage {
    name = "Build"

    on_failure {
      result = "RETRY"

      retry_configuration {
        retry_mode = "ALL_ACTIONS"
      }
    }

    action {
      name             = "Build"
      category         = "Build"
      owner            = "AWS"
      provider         = "CodeBuild"
      version          = "1"
      run_order        = 1
      namespace        = "BuildVariables"
      input_artifacts  = ["SourceArtifact"]
      output_artifacts = ["BuildArtifact"]
      region           = data.aws_region.current.name

      configuration = {
        ProjectName = aws_codebuild_project.web.name
      }
    }
  }

  trigger {
    provider_type = "CodeStarSourceConnection"

    git_configuration {
      source_action_name = "Source"

      push {
        branches {
          includes = ["dev"]
        }
        file_paths {
          includes = ["web/**"]
        }
      }
    }
  }
}
