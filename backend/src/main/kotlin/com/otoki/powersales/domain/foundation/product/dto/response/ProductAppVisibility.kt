package com.otoki.powersales.domain.foundation.product.dto.response

/**
 * 제품의 모바일 앱 노출/주문 가능 진단 결과.
 *
 * 판정 주체는 [com.otoki.powersales.domain.foundation.product.service.ProductAppVisibilityEvaluator] 이며,
 * 검색 쿼리(`orderableProductFilter`) / 주문 서버 가드와 동일한 술어를 사용한다.
 *
 * @property searchable 앱 제품검색(주문서 제품검색 탭 / 제품조회 / 바코드 스캔)에 노출되는지
 * @property orderable 주문서에 담을 수 있는지 (전용상품 / 시식·증정용이 아닌지)
 * @property issues 노출/주문을 막는 사유 전량. 정상이면 빈 목록
 */
data class ProductAppVisibility(
    val searchable: Boolean,
    val orderable: Boolean,
    val issues: List<ProductAppVisibilityIssue>
)

/**
 * 노출/주문 차단 사유 1건.
 *
 * @property code 사유 식별자 (화면 분기 / 집계용)
 * @property scope [SCOPE_SEARCH] = 검색 자체에서 제외 / [SCOPE_ORDER] = 검색은 되나 주문 담기 불가
 * @property message 무엇이 문제인지 — 현재 값을 포함한다
 * @property action 운영자가 다음에 확인할 곳 — 어느 시스템의 어느 값인지까지
 */
data class ProductAppVisibilityIssue(
    val code: String,
    val scope: String,
    val message: String,
    val action: String
) {
    companion object {
        const val SCOPE_SEARCH: String = "SEARCH"
        const val SCOPE_ORDER: String = "ORDER"
    }
}
