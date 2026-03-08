package com.otoki.internal.promotion.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class PromotionNotFoundException : BusinessException(
    errorCode = "PROMOTION_NOT_FOUND",
    message = "행사마스터를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class InvalidDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "종료일이 시작일보다 이전입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class CostCenterNotFoundException : BusinessException(
    errorCode = "COST_CENTER_NOT_FOUND",
    message = "생성자의 지점코드가 존재하지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AccountNotFoundException : BusinessException(
    errorCode = "ACCOUNT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ProductNotFoundException : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = "상품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class PromotionForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class PromotionInvalidParameterException : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = "유효하지 않은 파라미터",
    httpStatus = HttpStatus.BAD_REQUEST
)
