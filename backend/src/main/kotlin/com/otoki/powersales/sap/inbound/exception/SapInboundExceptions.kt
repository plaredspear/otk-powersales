package com.otoki.powersales.sap.inbound.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * SAP 인바운드 페이로드 검증 실패 (예: 행 전체 null).
 * 트랜잭션 전체를 롤백하고 422 INVALID_PAYLOAD 로 응답한다.
 */
class SapInvalidPayloadException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_PAYLOAD",
    message = detail ?: "요청 페이로드가 올바르지 않습니다",
    httpStatus = HttpStatus.UNPROCESSABLE_ENTITY
)
