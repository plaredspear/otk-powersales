package com.otoki.powersales.domain.activity.order.exception

import com.otoki.powersales.platform.common.exception.BusinessException

import org.springframework.http.HttpStatus
import java.math.BigDecimal

/**
 * 잘못된 주문 파라미터
 */
class InvalidOrderParameterException(detail: String) : BusinessException(
    errorCode = "ORD_INVALID_PARAM",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 잘못된 납기일 범위
 */
class InvalidDateRangeException : BusinessException(
    errorCode = "ORD_INVALID_DATE_RANGE",
    message = "납기일 종료일은 시작일 이후여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 납기일 기간이 7일 한도를 초과한 경우 (레거시 동등 안전장치).
 */
class OrderDateRangeTooWideException : BusinessException(
    errorCode = "ORD_DATE_RANGE_TOO_WIDE",
    message = "기간은 최대 7일까지 검색할 수 있습니다.",
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * 주문을 찾을 수 없음
 */
class OrderNotFoundException : BusinessException(
    errorCode = "ORDER_NOT_FOUND",
    message = "주문을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 다른 사용자의 주문에 접근 시도
 */
class ForbiddenOrderAccessException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 재전송 불가한 주문 상태
 */
class InvalidOrderStatusException : BusinessException(
    errorCode = "INVALID_ORDER_STATUS",
    message = "전송실패 상태의 주문만 재전송할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 마감된 주문에 대한 변경 시도 (재전송, 취소 등)
 */
class OrderAlreadyClosedException(detail: String = "마감된 주문은 변경할 수 없습니다") : BusinessException(
    errorCode = "ORDER_ALREADY_CLOSED",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 이미 취소된 제품이 포함된 요청
 */
class AlreadyCancelledException(productCodes: List<String>) : BusinessException(
    errorCode = "ALREADY_CANCELLED",
    message = "이미 취소된 제품이 포함되어 있습니다: ${productCodes.joinToString(", ")}",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 해당 주문에 포함되지 않은 제품코드
 */
class ProductNotInOrderException(productCodes: List<String>) : BusinessException(
    errorCode = "PRODUCT_NOT_IN_ORDER",
    message = "해당 주문에 포함되지 않은 제품입니다: ${productCodes.joinToString(", ")}",
    httpStatus = HttpStatus.BAD_REQUEST
)

// ───── Spec #592 — 주문 등록 예외 ─────

/**
 * 본인 담당 거래처가 아닌 경우 (`account.employee_id != JWT 사번`).
 */
class OrderAccountForbiddenException : BusinessException(
    errorCode = "ORD_ACCOUNT_FORBIDDEN",
    message = "본인 담당 거래처가 아닙니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 여신 한도 초과 (서버 재검증 시 `creditBalance < totalAmount`).
 */
class OrderLoanExceededException(creditBalance: BigDecimal, totalAmount: BigDecimal) : BusinessException(
    errorCode = "ORD_LOAN_EXCEEDED",
    message = "여신 한도를 초과했습니다 (한도: $creditBalance / 요청: $totalAmount)",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 공급제한 초과 (`InventorySearch.SupplyLimitQTY < quantityPieces`).
 */
class OrderProductRestrictedException(productCode: String, limit: Int, requested: Int) : BusinessException(
    errorCode = "ORD_PRODUCT_RESTRICTED",
    message = "공급제한을 초과한 제품이 있습니다 (productCode: $productCode, limit: $limit, requested: $requested)",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 단위 환산 정합 위반 또는 unit enum 위반.
 */
class OrderInvalidUnitException(detail: String) : BusinessException(
    errorCode = "ORD_INVALID_UNIT",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 입력값 형식 검증 위반 (라인 누락 / 음수 / 미래 일자 / 제품 마스터 미등록 등).
 */
class OrderInvalidRequestException(detail: String) : BusinessException(
    errorCode = "ORD_INVALID_REQUEST",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 주문 등록/재전송 일시 차단 (운영(prod) 환경 임시 중단).
 *
 * 운영 환경에서 주문 등록·재전송을 임시로 막기 위한 서버 가드. 활성 프로파일에 `prod` 가 포함되면
 * 주문 등록/재전송 진입부에서 즉시 던져 SAP 호출·DB 적재 이전에 전면 차단한다. dev/local 은 영향 없음.
 */
class OrderRegistrationBlockedException : BusinessException(
    errorCode = "ORD_REGISTRATION_BLOCKED",
    message = "현재 주문 등록이 일시 중단되었습니다. 잠시 후 다시 시도해 주세요.",
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
)

/**
 * 주문 등록 마감 시각 초과 (server-side 마감 가드).
 *
 * 레거시 `OrderController.java:485-498` (reqOrder `dateConfirm`) 동등 — 마감 시각
 * `(납기일 - 1일) 13:50 KST` 을 넘긴 등록 시도. 레거시는 Heroku 사전 차단(`RESULT_CODE='DE'`)으로만
 * 막았으나, 모바일 검증 우회 직접 호출에 대비해 backend 가 server-side 로 재검증한다.
 */
class OrderDeadlinePassedException : BusinessException(
    errorCode = "ORD_DEADLINE_PASSED",
    message = "마감시간이 지났습니다. 납기일이 내일인 경우 1시50분까지 주문이 가능합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)
