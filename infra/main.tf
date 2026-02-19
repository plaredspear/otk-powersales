provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

################################################################################
# Networking
################################################################################

module "networking" {
  source = "./modules/networking"

  project     = var.project
  environment = var.environment

  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
}

################################################################################
# ECR
################################################################################

module "ecr" {
  source = "./modules/ecr"

  project     = var.project
  environment = var.environment
}

################################################################################
# RDS
################################################################################

module "rds" {
  source = "./modules/rds"

  project     = var.project
  environment = var.environment

  subnet_ids        = module.networking.private_subnet_ids
  security_group_id = module.networking.rds_security_group_id

  instance_class    = var.rds_instance_class
  engine_version    = var.rds_engine_version
  allocated_storage = var.rds_allocated_storage
  multi_az          = var.rds_multi_az

  db_name     = var.db_name
  db_username = var.db_username
  db_password = var.db_password
}

################################################################################
# Secrets Manager
################################################################################

module "secrets" {
  source = "./modules/secrets"

  project     = var.project
  environment = var.environment

  db_username = var.db_username
  db_password = var.db_password
  db_host     = module.rds.address
  db_port     = module.rds.port
  db_name     = var.db_name

  jwt_secret = var.jwt_secret
}

################################################################################
# DNS + ACM (certificate created BEFORE ALB to avoid circular dependency)
################################################################################

module "dns" {
  source = "./modules/dns"

  project     = var.project
  environment = var.environment

  domain_name    = var.domain_name
  hosted_zone_id = var.hosted_zone_id
}

################################################################################
# ALB (depends on DNS for certificate)
################################################################################

module "alb" {
  source = "./modules/alb"

  project     = var.project
  environment = var.environment

  vpc_id            = module.networking.vpc_id
  public_subnet_ids = module.networking.public_subnet_ids
  security_group_id = module.networking.alb_security_group_id
  certificate_arn   = module.dns.certificate_validation_arn
}

################################################################################
# Route53 A Record (depends on ALB — here to avoid circular dependency)
################################################################################

resource "aws_route53_record" "api" {
  zone_id = var.hosted_zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = module.alb.alb_dns_name
    zone_id                = module.alb.alb_zone_id
    evaluate_target_health = true
  }
}

################################################################################
# ECS
################################################################################

module "ecs" {
  source = "./modules/ecs"

  project     = var.project
  environment = var.environment
  region      = var.region

  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.ecs_security_group_id
  target_group_arn   = module.alb.target_group_arn

  ecr_repository_url = module.ecr.repository_url
  task_cpu           = var.ecs_task_cpu
  task_memory        = var.ecs_task_memory
  desired_count      = var.ecs_desired_count

  db_host = module.rds.address
  db_port = module.rds.port
  db_name = var.db_name

  db_credentials_arn = module.secrets.db_credentials_arn
  jwt_secret_arn     = module.secrets.jwt_secret_arn
  secrets_arns = [
    module.secrets.db_credentials_arn,
    module.secrets.jwt_secret_arn,
  ]
}

################################################################################
# CI/CD (CodeBuild + GitLab OIDC)
################################################################################

module "cicd" {
  source = "./modules/cicd"

  project        = var.project
  environment    = var.environment
  region         = var.region
  aws_account_id = var.aws_account_id

  gitlab_project_path  = var.gitlab_project_path
  gitlab_deploy_branch = var.gitlab_deploy_branch

  ecr_repository_url = module.ecr.repository_url
  ecr_repository_arn = module.ecr.repository_arn
  ecs_cluster_name   = module.ecs.cluster_name
  ecs_service_name   = module.ecs.service_name

  ecs_task_role_arns = [
    module.ecs.task_execution_role_arn,
    module.ecs.task_role_arn,
  ]
}
