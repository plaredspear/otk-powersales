package com.otoki.powersales.external.sap.auth.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class SapInvalidClientException : BusinessException(
    errorCode = "INVALID_CLIENT",
    message = "클라이언트 인증 실패",
    httpStatus = HttpStatus.UNAUTHORIZED
)

class SapUnsupportedGrantTypeException : BusinessException(
    errorCode = "UNSUPPORTED_GRANT_TYPE",
    message = "지원하지 않는 grant_type",
    httpStatus = HttpStatus.BAD_REQUEST
)

class SapInvalidScopeException : BusinessException(
    errorCode = "INVALID_SCOPE",
    message = "허용되지 않은 scope",
    httpStatus = HttpStatus.BAD_REQUEST
)

class SapIpNotAllowedException : BusinessException(
    errorCode = "IP_NOT_ALLOWED",
    message = "허용되지 않은 IP",
    httpStatus = HttpStatus.FORBIDDEN
)

class SapInvalidTokenException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_TOKEN",
    message = detail ?: "토큰이 필요합니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

class SapInsufficientScopeException : BusinessException(
    errorCode = "INSUFFICIENT_SCOPE",
    message = "권한 없음",
    httpStatus = HttpStatus.FORBIDDEN
)

class SapSanityCheckFailedException(detail: String? = null) : BusinessException(
    errorCode = "SANITY_CHECK_FAILED",
    message = detail ?: "데이터 정합성 검증 실패",
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY
)
