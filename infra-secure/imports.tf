###############################################################################
# Import blocks — 변수 기반 조건부 import
#
# 사용법:
# - 신규 구축: `var.reuse_web_stack` 비워두면 블록은 no-op (신규 create)
# - 재구축(잔존 CloudFront/OAC/ACM 흡수): terraform.tfvars 에 ID/ARN 채우면
#   terraform apply 시 자동 import 후 나머지 리소스와 함께 처리
#
# destroy 전엔 CloudFront/OAC/ACM(web_dev) 를 `terraform state rm` 으로
# state 에서 제거해 두어야 destroy 가 완료된다 (AWS 측 실제 삭제는 월말 이후).
###############################################################################

import {
  for_each = var.reuse_web_stack.distribution_id == null ? toset([]) : toset([var.reuse_web_stack.distribution_id])
  to       = aws_cloudfront_distribution.web
  id       = each.value
}

import {
  for_each = var.reuse_web_stack.oac_id == null ? toset([]) : toset([var.reuse_web_stack.oac_id])
  to       = aws_cloudfront_origin_access_control.web
  id       = each.value
}

import {
  for_each = var.reuse_web_stack.acm_cert_arn == null ? toset([]) : toset([var.reuse_web_stack.acm_cert_arn])
  provider = aws.us_east_1
  to       = aws_acm_certificate.web_dev
  id       = each.value
}
