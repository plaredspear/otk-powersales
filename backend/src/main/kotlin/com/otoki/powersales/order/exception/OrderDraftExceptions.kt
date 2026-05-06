package com.otoki.powersales.order.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 주문 임시저장 입력 검증 위반 (Spec #596). 라인 누락 / 단위 / productCode / quantity 음수 등.
 */
class OrderDraftInvalidRequestException(detail: String) : BusinessException(
    errorCode = "ORD_DRAFT_INVALID_REQUEST",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * 본인 담당 거래처가 아닌 임시저장 시도 (Spec #596).
 */
class OrderDraftAccountForbiddenException : BusinessException(
    errorCode = "ORD_DRAFT_ACCOUNT_FORBIDDEN",
    message = "본인 담당 거래처가 아닙니다",
    httpStatus = HttpStatus.FORBIDDEN,
)
