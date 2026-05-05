package com.otoki.powersales.order.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * SAP `LoanInquiry` 응답 `resultCode != 'S'` (거래처 미존재 포함, Q7 단일 분기) — Spec #594.
 *
 * 메시지는 SAP `resutlMsg` (오타 그대로) 패스스루.
 */
class LoanSapErrorException(detail: String?) : BusinessException(
    errorCode = "LOAN_SAP_ERROR",
    message = detail ?: "SAP 응답 오류",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
)

/**
 * SAP 응답 본문에 `<` 포함 (HTML 에러 페이지) — `SapResponseHtmlGuard` 차단.
 * 레거시 `IF_Util.cls:191-198` 계승.
 */
class LoanSapHtmlResponseException : BusinessException(
    errorCode = "LOAN_SAP_HTML_RESPONSE",
    message = "SAP 응답 형식 오류",
    httpStatus = HttpStatus.BAD_GATEWAY
)

/**
 * SAP 호출 타임아웃(120초) 또는 5xx 응답.
 */
class LoanSapUnavailableException(detail: String? = null) : BusinessException(
    errorCode = "LOAN_SAP_UNAVAILABLE",
    message = detail ?: "SAP 시스템 연결 실패",
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE
)
