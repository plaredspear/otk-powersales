###############################################################################
# Application configuration — SSM Parameter Store
#
# Terraform 이 관리하는 동적/stage-의존 값들을 /{project}/{stage}/... 경로 아래
# 에 publish 한다. buildspec 이 CodeBuild 시 이 값들을 조회해 .ebextensions/
# env.config 를 생성하고, EB 배포 시 application environment 로 주입된다.
#
# 비밀(Password) 은 여기 저장하지 않는다. RDS master user 비밀번호는 RDS 가
# 자동 발급/관리하는 Secrets Manager secret 을 그대로 사용하고, 그 ARN 만
# non-secret 으로 publish → 앱이 필요 시 런타임에 직접 조회한다.
###############################################################################

resource "aws_ssm_parameter" "rds_host" {
  name  = "${local.param_prefix}/rds/host"
  type  = "String"
  value = aws_db_instance.main.address
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "rds_port" {
  name  = "${local.param_prefix}/rds/port"
  type  = "String"
  value = tostring(aws_db_instance.main.port)
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "rds_username" {
  name  = "${local.param_prefix}/rds/username"
  type  = "String"
  value = aws_db_instance.main.username
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "rds_secret_arn" {
  name  = "${local.param_prefix}/rds/secret-arn"
  type  = "String"
  value = aws_db_instance.main.master_user_secret[0].secret_arn
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "redis_endpoint" {
  name  = "${local.param_prefix}/redis/endpoint"
  type  = "String"
  value = aws_elasticache_replication_group.redis.primary_endpoint_address
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "redis_port" {
  name  = "${local.param_prefix}/redis/port"
  type  = "String"
  value = tostring(aws_elasticache_replication_group.redis.port)
  tags  = local.common_tags
}
