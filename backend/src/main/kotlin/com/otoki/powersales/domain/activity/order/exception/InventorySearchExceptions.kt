package com.otoki.powersales.domain.activity.order.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * SAP `InventorySearch` (SD03070) 가 **정상 응답(HTTP 200 + JSON)** 으로 업무 규칙상 요청을 거부한 경우
 * (`resultCode != 'S'`). 예: "출고정지 고객 입니다", "조회 가능한 거래처가 아닙니다".
 *
 * 서버 결함이 아니라 클라이언트 요청이 업무 규칙에 걸린 것이므로 4xx 로 내린다. 여신 초과
 * ([OrderLoanExceededException]) / 전용상품 차단([OrderInvalidRequestException]) 과 같은 부류다.
 * `serverFault` 는 4xx 라 기본값 `false` — `GlobalExceptionHandler` 가 스택 없이 `warn` 요약만 남긴다
 * (이 경로의 스택은 Controller→Service→Sender 로 항상 동일해 진단에 기여하지 않고, 실제 서버 결함
 * 로그를 묻어버리는 노이즈가 된다).
 *
 * SAP 응답 자체가 깨진 경우(본문 공백 / JSON 파싱 실패 / `result` 누락) 는 서버·연동 결함이므로
 * [InventorySapErrorException] 로 구분해 5xx + 스택을 유지한다.
 */
class InventorySapRejectedException(detail: String?) : BusinessException(
    errorCode = "INVENTORY_SAP_REJECTED",
    message = detail ?: "SAP 재고조회 요청이 거부되었습니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)

/**
 * SAP `InventorySearch` (SD03070) 응답 자체가 처리 불가인 경우 — 본문이 비어 있거나, JSON 파싱에
 * 실패했거나, `result` 필드가 누락된 구조 이상. 연동 계약이 깨진 상태라 스택트레이스까지 남겨 추적한다.
 *
 * SAP 가 정상 응답으로 업무 규칙상 거부한 경우는 [InventorySapRejectedException] 를 쓴다.
 */
class InventorySapErrorException(detail: String?) : BusinessException(
    errorCode = "INVENTORY_SAP_ERROR",
    message = detail ?: "SAP 재고조회 응답 오류",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
)

/**
 * SAP 가 JSON 대신 HTML(인증/프록시 오류 페이지) 을 반환한 경우.
 */
class InventorySapHtmlResponseException : BusinessException(
    errorCode = "INVENTORY_SAP_HTML_RESPONSE",
    message = "SAP 재고조회 응답 형식 오류",
    httpStatus = HttpStatus.BAD_GATEWAY,
)

/**
 * SAP 연결 실패(HTTP 오류 / 네트워크 오류). 주문 등록 사전 검증을 진행할 수 없음.
 */
class InventorySapUnavailableException(detail: String? = null) : BusinessException(
    errorCode = "INVENTORY_SAP_UNAVAILABLE",
    message = detail ?: "SAP 시스템 연결 실패",
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
)
