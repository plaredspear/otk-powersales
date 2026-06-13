package com.otoki.powersales.external.sf.auth.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class SfInvalidClientException : BusinessException(
    errorCode = "INVALID_CLIENT",
    message = "클라이언트 인증 실패",
    httpStatus = HttpStatus.UNAUTHORIZED
)

class SfUnsupportedGrantTypeException : BusinessException(
    errorCode = "UNSUPPORTED_GRANT_TYPE",
    message = "지원하지 않는 grant_type",
    httpStatus = HttpStatus.BAD_REQUEST
)

class SfInvalidScopeException : BusinessException(
    errorCode = "INVALID_SCOPE",
    message = "허용되지 않은 scope",
    httpStatus = HttpStatus.BAD_REQUEST
)

class SfInvalidTokenException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_TOKEN",
    message = detail ?: "토큰이 필요합니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

class SfInsufficientScopeException : BusinessException(
    errorCode = "INSUFFICIENT_SCOPE",
    message = "권한 없음",
    httpStatus = HttpStatus.FORBIDDEN
)
