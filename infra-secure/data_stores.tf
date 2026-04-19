###############################################################################
# Data stores — RDS (PostgreSQL) and ElastiCache (Valkey, standalone)
#
# Subnets: data_a, data_c (private tier, no internet route)
# Access:  eb SG → rds SG (5432) / redis SG (6379)
###############################################################################

# ---------------------------------------------------------------------------
# RDS
# ---------------------------------------------------------------------------
resource "aws_db_subnet_group" "main" {
  name        = "${local.name_prefix}-db-subnet-group"
  description = "RDS subnet group for ${local.name_prefix} (data-a, data-c)"
  subnet_ids  = [aws_subnet.data_a.id, aws_subnet.data_c.id]

  tags = {
    Name = "${local.name_prefix}-db-subnet-group"
  }
}

resource "aws_db_instance" "main" {
  identifier     = "${local.name_prefix}-db"
  engine         = "postgres"
  engine_version = "17.6"
  instance_class = "db.t4g.micro"

  allocated_storage     = 20
  max_allocated_storage = 1000
  storage_type          = "gp3"
  storage_encrypted     = true
  kms_key_id            = "arn:aws:kms:ap-northeast-2:${data.aws_caller_identity.current.account_id}:key/943da723-0f48-4c5f-9f2a-f0849b657969"

  db_name                     = "otoki"
  username                    = var.rds_master_username
  manage_master_user_password = true
  port                        = 5432

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  parameter_group_name   = "default.postgres17"
  ca_cert_identifier     = "rds-ca-rsa2048-g1"

  multi_az                     = false
  publicly_accessible          = false
  auto_minor_version_upgrade   = true
  performance_insights_enabled = true
  performance_insights_kms_key_id = "arn:aws:kms:ap-northeast-2:${data.aws_caller_identity.current.account_id}:key/943da723-0f48-4c5f-9f2a-f0849b657969"

  backup_retention_period = 3
  backup_window           = "15:42-16:12"
  maintenance_window      = "thu:16:28-thu:16:58"
  copy_tags_to_snapshot   = true
  deletion_protection     = false
  skip_final_snapshot     = true

}

# ---------------------------------------------------------------------------
# ElastiCache (Valkey, standalone — cluster mode off, TLS off)
# ---------------------------------------------------------------------------
resource "aws_elasticache_subnet_group" "redis" {
  name        = "${local.name_prefix}-redis-subnet-group"
  description = " "
  subnet_ids  = [aws_subnet.data_a.id, aws_subnet.data_c.id]
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name_prefix}-redis"
  description          = "${local.name_prefix} redis (standalone)"

  engine         = "valkey"
  engine_version = "8.2"
  node_type      = "cache.t4g.micro"
  port           = 6379

  num_cache_clusters = 1

  parameter_group_name = "default.valkey8"
  subnet_group_name    = aws_elasticache_subnet_group.redis.name
  security_group_ids   = [aws_security_group.redis.id]

  automatic_failover_enabled = false
  multi_az_enabled           = false
  auto_minor_version_upgrade = true

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false

  snapshot_retention_limit = 0
  snapshot_window          = "01:00-02:00"
  maintenance_window       = "tue:07:00-tue:08:00"

  apply_immediately = false

  lifecycle {
    ignore_changes = [auth_token, auth_token_update_strategy]
  }
}
