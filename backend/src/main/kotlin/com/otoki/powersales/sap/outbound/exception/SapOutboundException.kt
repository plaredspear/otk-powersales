package com.otoki.powersales.sap.outbound.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class SapOutboundException(
    detail: String,
    val httpStatusCode: Int? = null,
    cause: Throwable? = null
) : BusinessException(
    errorCode = "SAP_OUTBOUND_FAILED",
    message = "SAP 송신에 실패했습니다: $detail",
    httpStatus = HttpStatus.BAD_GATEWAY,
    cause = cause
)
