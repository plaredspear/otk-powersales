/*
package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/ **
 * 잘못된 주문 파라미터
 * /
class InvalidOrderParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 잘못된 납기일 범위
 * /
class InvalidDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "납기일 종료일은 시작일 이후여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 주문을 찾을 수 없음
 * /
class OrderNotFoundException : BusinessException(
    errorCode = "ORDER_NOT_FOUND",
    message = "주문을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/ **
 * 다른 사용자의 주문에 접근 시도
 * /
class ForbiddenOrderAccessException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/ **
 * 재전송 불가한 주문 상태
 * /
class InvalidOrderStatusException : BusinessException(
    errorCode = "INVALID_ORDER_STATUS",
    message = "전송실패 상태의 주문만 재전송할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 마감된 주문에 대한 변경 시도 (재전송, 취소 등)
 * /
class OrderAlreadyClosedException(detail: String = "마감된 주문은 변경할 수 없습니다") : BusinessException(
    errorCode = "ORDER_ALREADY_CLOSED",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 이미 취소된 제품이 포함된 요청
 * /
class AlreadyCancelledException(productCodes: List<String>) : BusinessException(
    errorCode = "ALREADY_CANCELLED",
    message = "이미 취소된 제품이 포함되어 있습니다: ${productCodes.joinToString(", ")}",
    httpStatus = HttpStatus.BAD_REQUEST
)

/ **
 * 해당 주문에 포함되지 않은 제품코드
 * /
class ProductNotInOrderException(productCodes: List<String>) : BusinessException(
    errorCode = "PRODUCT_NOT_IN_ORDER",
    message = "해당 주문에 포함되지 않은 제품입니다: ${productCodes.joinToString(", ")}",
    httpStatus = HttpStatus.BAD_REQUEST
)
*/
