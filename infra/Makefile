# Usage:
#   make init-dev       # Initialize dev backend
#   make plan-dev       # Plan dev changes
#   make apply-dev      # Apply dev changes
#   make destroy-dev    # Destroy dev resources
#
# Secrets: create envs/secrets.tfvars from envs/secrets.tfvars.example

.PHONY: init-dev plan-dev apply-dev destroy-dev

init-dev:
	terraform init -backend-config=envs/dev.backend.hcl -reconfigure

plan-dev:
	terraform plan -var-file=envs/dev.tfvars -var-file=envs/secrets.tfvars

apply-dev:
	terraform apply -var-file=envs/dev.tfvars -var-file=envs/secrets.tfvars

destroy-dev:
	terraform destroy -var-file=envs/dev.tfvars -var-file=envs/secrets.tfvars
