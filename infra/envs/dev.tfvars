# General
project        = "otoki"
environment    = "dev"
region         = "ap-northeast-2"
aws_account_id = "983241034734"

# Networking
vpc_cidr             = "10.0.0.0/16"
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]
availability_zones   = ["ap-northeast-2a", "ap-northeast-2c"]

# RDS
rds_instance_class    = "db.t3.micro"
rds_engine_version    = "16.6"
rds_allocated_storage = 20
rds_multi_az          = false
db_name               = "otoki"
db_username           = "otoki_admin"

# ElastiCache
elasticache_node_type = "cache.t4g.micro"

# EC2 (ECS EC2 launch type)
ec2_instance_type        = "t3.small"
ec2_asg_min_size         = 1
ec2_asg_max_size         = 3
ec2_asg_desired_capacity = 1

# ECS
ecs_container_memory             = 896
ecs_container_memory_reservation = 512
ecs_desired_count                = 1

# DNS — 실제 값으로 교체 필요
domain_name    = "dev-pwrs-api.codapt.kr"
db_domain_name = "dev-db.codapt.kr"
hosted_zone_id = "Z02189752GJTU3DRB1DOX"

# RDS Public Access (dev only)
rds_publicly_accessible = true

# CI/CD
github_repo           = "plaredspear/otk-powersales"
github_repository_url = "https://github.com/plaredspear/otk-powersales.git"
github_deploy_branch  = "main"
