package com.otoki.powersales.domain.activity.order.util

/**
 * 주문 라인 수 상한.
 *
 * 모바일 주문서 작성 화면이 제품 담기 / 제출 양 시점에 100개로 막고 있는 것
 * (`order_form_provider.dart` `_tryAddProductLine` / `addProductToOrder` / 제출 검증 (F))과
 * 동일한 상한을 서버에서도 강제한다. 화면 차단을 우회하는 경로(구버전 앱, API 직접 호출,
 * 임시저장 복원)를 방어하는 것이 목적이다.
 */
object OrderLineLimits {

    /** 1건의 주문(및 임시저장)에 담을 수 있는 최대 제품 라인 수. */
    const val MAX_ORDER_LINES: Int = 100

    /** 상한 초과 시 사용자에게 노출되는 문구 — 모바일 안내 문구와 동일하게 유지한다. */
    const val MAX_ORDER_LINES_MESSAGE: String = "${MAX_ORDER_LINES}개 이하로 등록해주세요"
}
