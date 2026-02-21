resource "aws_ssm_parameter" "output" {
  for_each = var.outputs

  name  = "/${var.project}/${var.environment}/infra/${each.key}"
  type  = "String"
  value = each.value
  tier  = "Standard"

  tags = {
    Name = "${var.project}-${var.environment}-${each.key}"
  }
}
