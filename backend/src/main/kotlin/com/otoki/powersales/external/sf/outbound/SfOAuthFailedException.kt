package com.otoki.powersales.external.sf.outbound

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * SF OAuth 토큰 발급 실패 — Spec #829.
 *
 * 토큰 endpoint 가 401/non-2xx 응답을 반환하거나 응답에 `access_token` 이 없을 때 발생.
 * Service 가 catch 하여 `status = SEND_FAILED` 로 변환 — Controller 는 HTTP 200 응답.
 */
class SfOAuthFailedException(detail: String) : BusinessException(
    errorCode = "SF_OAUTH_FAILED",
    message = "SF OAuth 토큰 발급에 실패했습니다: $detail",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
)
