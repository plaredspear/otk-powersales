package com.otoki.powersales.domain.activity.order.exception

import com.otoki.powersales.platform.common.exception.BusinessException

import org.springframework.http.HttpStatus

/**
 * 거래처를 찾을 수 없음
 */
class ClientNotFoundException : BusinessException(
    errorCode = "CLIENT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 거래처 접근 권한 없음 (담당 거래처가 아닌 경우)
 */
class ForbiddenClientAccessException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * SAP 주문번호 형식 오류 — 숫자 1~20자가 아닌 입력
 */
class InvalidSapOrderNumberException : BusinessException(
    errorCode = "ORD_INVALID_SAP_NUMBER",
    message = "SAP 주문번호 형식이 올바르지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * SAP 주문번호로 주문을 찾을 수 없음
 */
class SapOrderNotFoundException : BusinessException(
    errorCode = "ORD_SAP_NOT_FOUND",
    message = "주문을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
