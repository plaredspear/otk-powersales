package com.otoki.powersales.domain.activity.order.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * SAP `InventorySearch` (SD03070) 응답이 실패 코드(`resultCode != 'S'`) 이거나
 * 본문 파싱에 실패한 경우. 레거시 `IF_REST_MOBILE_InventorySearch` 의 `RESULT_CODE='0'` 동등.
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
