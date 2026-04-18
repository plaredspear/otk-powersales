locals {
  name_prefix = "${var.stage}-${var.project}"

  api_fqdn = "${var.api_subdomain}.${var.domain}"
  web_fqdn = "${var.web_subdomain}.${var.domain}"

  # SSM Parameter Store 경로 prefix — 앱 구성(RDS/Redis endpoint, secret ARN 등)
  # 은 /{project}/{stage}/... 하위에 publish 되고 Spring Boot 은 빌드시 생성된
  # .ebextensions/env.config 를 통해 env var 로 수신한다.
  param_prefix = "/${var.project}/${var.stage}"

  common_tags = {
    Project   = var.project
    Stage     = var.stage
    ManagedBy = "terraform"
  }
}
