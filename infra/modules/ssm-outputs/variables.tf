variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "outputs" {
  description = "Map of output key → value to store in SSM Parameter Store"
  type        = map(string)
}
