package com.otoki.powersales.domain.activity.order.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 주문 취소 마감 시각 초과 (Spec #597).
 *
 * 레거시 `view.jsp:214` `<c:if test="${type eq 'before'}">` UI 차단을 서버 강제 검증으로 보강.
 * 룰: `now + 1일 < 납기일 13:50 KST` 통과 시에만 취소 가능.
 */
class OrderCancelDeadlinePassedException : BusinessException(
    errorCode = "ORD_CANCEL_DEADLINE_PASSED",
    message = "주문 취소 마감 시각이 지났습니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * 주문 취소 불가 상태 (Spec #597).
 *
 * `OrderRequestStatus` 가 `SENT` / `APPROVED` / `SEND_FAILED` 가 아닌 경우 (`DRAFT`/`CANCELED` 거부).
 */
class OrderCancelInvalidStatusException(status: String) : BusinessException(
    errorCode = "ORD_CANCEL_INVALID_STATUS",
    message = "취소할 수 없는 주문 상태입니다: $status",
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * 취소 요청 라인 ID 가 해당 주문에 속하지 않음 (Spec #597).
 */
class OrderCancelLineNotFoundException(invalidIds: List<Long>) : BusinessException(
    errorCode = "ORD_CANCEL_LINE_NOT_FOUND",
    message = "해당 주문에 속하지 않는 라인이 포함되어 있습니다: ${invalidIds.joinToString(", ")}",
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * SAP 동기 송신 실패 (Spec #597).
 *
 * 응답 `'E'` / timeout / HTML 가드 실패 시. DB 무변경. 사용자 재시도 가능.
 * 레거시 `IF_REST_MOBILE_OrderCancelRequest.cls:105-114` 동등 — `'S'` 가 아니면 라인 상태 변경 안 함.
 */
class OrderCancelSapFailedException(detail: String? = null) : BusinessException(
    errorCode = "ORD_CANCEL_SAP_FAILED",
    message = detail ?: "SAP 송신에 실패했습니다",
    httpStatus = HttpStatus.BAD_GATEWAY,
)
