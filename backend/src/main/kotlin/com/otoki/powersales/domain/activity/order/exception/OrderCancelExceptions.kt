package com.otoki.powersales.domain.activity.order.exception

import com.otoki.powersales.platform.common.exception.BusinessException
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
 * `OrderRequestStatus` 가 `SENT` / `APPROVED` / `SEND_FAILED` 가 아닌 경우 (`DRAFT`/`CANCEL_REQUESTED` 거부).
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
 *
 * [rejected] — 실패 확실성 구분:
 *  - `true`: SAP 가 HTTP 200 으로 응답했으나 `resultCode != 'S'` 로 **취소를 명시적으로 거부**한 경우.
 *    SAP 에 요청이 도달했고 확정적으로 거절됐음이 분명하므로, 호출자는 `cancel_requested_at` 흔적을
 *    롤백해 "취소요청중" 오표시를 제거한다.
 *  - `false`: timeout / 네트워크 오류 / HTTP 4xx·5xx / HTML·빈 본문·JSON 파싱 실패 등 **결과 불확실**.
 *    실제로 SAP 에 반영됐을 수 있으므로 흔적을 유지해 상세조회 정합 근거로 남긴다 (기존 동작).
 *    ⚠️ 이 그룹에는 "timeout 났지만 SAP 내부에선 실제 'E' 거부된" 케이스도 섞여 있어, 그 경우
 *    흔적이 "취소요청중" 으로 잘못 남는다. timeout 은 최종 상태 확정 불가라 이 판정으로는 구분 못 함
 *    — 잔여 케이스 상세는 [com.otoki.powersales.domain.activity.order.service.OrderCancelService.cancel]
 *    catch 블록 주석 참조.
 */
class OrderCancelSapFailedException(
    detail: String? = null,
    val rejected: Boolean = false,
) : BusinessException(
    errorCode = "ORD_CANCEL_SAP_FAILED",
    message = detail ?: "SAP 송신에 실패했습니다",
    httpStatus = HttpStatus.BAD_GATEWAY,
)

/**
 * 등록 SAP 전송이 아직 진행 중이라 취소 보류 (Spec #597 경합 방어).
 *
 * 등록 outbox 가 `PENDING`/`RETRY`(SAP 미확정) 인 구간에 취소(삭제)가 나가면, 등록(생성) 메시지가
 * SAP 에 도달하기 전/재시도 중에 삭제가 먼저 도달하는 순서 역전이 생길 수 있다. 등록 outbox 가
 * `SENT`(SAP 응답 수신) 로 전이될 때까지 취소를 보류하며, 사용자는 잠시 후 재시도하면 된다.
 */
class OrderCancelInFlightException : BusinessException(
    errorCode = "ORD_CANCEL_IN_FLIGHT",
    message = "주문 등록 처리가 진행 중입니다. 잠시 후 다시 시도해 주세요.",
    httpStatus = HttpStatus.CONFLICT,
)
