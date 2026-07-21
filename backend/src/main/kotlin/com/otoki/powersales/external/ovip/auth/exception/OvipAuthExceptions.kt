package com.otoki.powersales.external.ovip.auth.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class OvipInvalidClientException : BusinessException(
    errorCode = "INVALID_CLIENT",
    message = "클라이언트 인증 실패",
    httpStatus = HttpStatus.UNAUTHORIZED
)

class OvipUnsupportedGrantTypeException : BusinessException(
    errorCode = "UNSUPPORTED_GRANT_TYPE",
    message = "지원하지 않는 grant_type",
    httpStatus = HttpStatus.BAD_REQUEST
)

class OvipInvalidTokenException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_TOKEN",
    message = detail ?: "토큰이 필요합니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

class OvipInvalidParameterException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail ?: "잘못된 요청 파라미터",
    httpStatus = HttpStatus.BAD_REQUEST
)
