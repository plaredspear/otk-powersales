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
 * 라인(제품) 단위 검증 위반 1건. 레거시 write.jsp 는 위반 제품 **행마다** 사유를 표시했으므로
 * (한 건만 알려주고 끝내지 않는다) 모바일이 제품 카드별로 표시할 수 있게 구조화해 내려준다.
 * `error.details.violations` 배열로 직렬화된다.
 */
data class OrderLineViolation(
    val productCode: String,
    val productName: String,
    val reason: Reason,
    /** 제품 카드 하단에 그대로 노출되는 사유 문구 (레거시 "공급제한수량 초과" 등). */
    val message: String,
    /** 환산수량(주문 단위) — 레거시 "최소주문단위 N개". */
    val minOrderQuantity: Int? = null,
    /** 공급 가능 수량(총 EA 환산) — 레거시 "공급 N개". */
    val supplyQuantity: Int? = null,
    /** 요청 수량(총 EA). */
    val requestedQuantity: Int? = null,
) {
    enum class Reason { INVALID_UNIT, SUPPLY_LIMIT_EXCEEDED }
}

/**
 * 라인별 위반 목록을 담는 예외 표식. GlobalExceptionHandler 가 `error.details.violations` 로 내려준다.
 */
interface OrderLineViolationsAware {
    val violations: List<OrderLineViolation>
}

/**
 * 공급제한 초과 (`InventorySearch.SupplyLimitQTY < quantityPieces`).
 *
 * 위반 라인을 **모두** 담는다(첫 건에서 중단하지 않음). 환산수량 위반이 섞여 있으면 그 건도 함께 담고
 * 코드는 공급제한(ORD_PRODUCT_RESTRICTED)으로 대표한다 — 모바일은 라인별 reason 으로 구분해 표시한다.
 */
class OrderProductRestrictedException(
    override val violations: List<OrderLineViolation>
) : BusinessException(
    errorCode = "ORD_PRODUCT_RESTRICTED",
    message = summarize(violations),
    httpStatus = HttpStatus.BAD_REQUEST
),
    OrderLineViolationsAware {
    companion object {
        private fun summarize(violations: List<OrderLineViolation>): String {
            val single = violations.singleOrNull()
                ?: return "주문할 수 없는 제품이 ${violations.size}건 있습니다"
            return "${single.productName} ${single.message}" +
                if (single.supplyQuantity != null && single.requestedQuantity != null) {
                    " (공급 ${single.supplyQuantity}개 / 요청 ${single.requestedQuantity}개)"
                } else {
                    ""
                }
        }
    }
}

/**
 * 단위 환산 정합 위반 또는 unit enum 위반.
 *
 * 환산 위반은 라인별 [violations] 를 함께 담아 모바일이 제품 카드마다 사유를 표시할 수 있게 한다
 * (unit enum 위반 등 라인 정보가 없는 경우는 빈 목록).
 */
class OrderInvalidUnitException(
    detail: String,
    override val violations: List<OrderLineViolation> = emptyList()
) : BusinessException(
    errorCode = "ORD_INVALID_UNIT",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
),
    OrderLineViolationsAware

/**
 * 입력값 형식 검증 위반 (라인 누락 / 음수 / 미래 일자 / 제품 마스터 미등록 등).
 */
class OrderInvalidRequestException(detail: String) : BusinessException(
    errorCode = "ORD_INVALID_REQUEST",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
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
