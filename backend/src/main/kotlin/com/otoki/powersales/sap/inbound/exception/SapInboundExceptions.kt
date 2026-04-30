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

/**
 * SAP 인바운드 페이로드 행 수 한도 초과. 413 PAYLOAD_TOO_LARGE. (Spec #560)
 */
class SapPayloadTooLargeException(maxRows: Int, received: Int) : BusinessException(
    errorCode = "PAYLOAD_TOO_LARGE",
    message = "한 요청 최대 ${maxRows}건 (수신: ${received})",
    httpStatus = HttpStatus.PAYLOAD_TOO_LARGE
)
