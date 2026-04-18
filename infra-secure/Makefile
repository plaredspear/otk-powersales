###############################################################################
# Terraform 래퍼 — destroy 시 잔존 리소스 자동 state rm
#
# CloudFront 배포는 생성된 당월 billing cycle 종료 전까지 삭제 불가이고, OAC/
# ACM(web_dev) 도 distribution 을 참조하는 동안 삭제 불가이다. 따라서 이들은
# destroy 대상에서 제외(state rm)하여 AWS 측에 남겨두고, 재구축 apply 때
# imports.tf + terraform.tfvars 의 reuse_web_stack 으로 흡수한다.
###############################################################################

# destroy 대상에서 제외할 terraform 리소스 주소 (AWS 리소스는 유지됨)
RETIRED_ADDRESSES := \
	aws_cloudfront_distribution.web \
	aws_cloudfront_origin_access_control.web \
	aws_acm_certificate.web_dev

# terraform 서브커맨드에 추가 전달할 플래그 (예: make apply ARGS=-auto-approve)
ARGS ?=

.PHONY: init plan apply destroy

init:
	terraform init $(ARGS)

plan:
	terraform plan $(ARGS)

apply:
	terraform apply $(ARGS)

destroy:
	@existing="$$(terraform state list 2>/dev/null)"; \
	for r in $(RETIRED_ADDRESSES); do \
		if echo "$$existing" | grep -qxF "$$r"; then \
			echo ">>> terraform state rm $$r"; \
			terraform state rm "$$r"; \
		fi; \
	done
	terraform destroy $(ARGS)
