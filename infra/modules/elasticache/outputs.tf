output "endpoint" {
  description = "ElastiCache primary endpoint address"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "port" {
  description = "ElastiCache port"
  value       = aws_elasticache_replication_group.main.port
}
