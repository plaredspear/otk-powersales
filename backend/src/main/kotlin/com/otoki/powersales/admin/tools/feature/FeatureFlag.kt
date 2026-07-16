package com.otoki.powersales.admin.tools.feature

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 로 on/off 하는 기능 토글(feature flag) 목록.
 *
 * 각 항목은 특정 등록(create) API 를 런타임에 차단/허용한다. 상태는 Redis 에 지속 저장되어
 * 앱 재시작 후에도 유지되며(로그 레벨과 달리 임시 조정이 아님), 미설정 시 기본은 **활성**이다.
 *
 * [code] 는 Redis key 와 API 계약(웹/모바일)에 노출되는 안정 식별자이므로 함부로 바꾸지 않는다.
 * [label] 은 관리자 화면 및 차단 안내 문구에 쓰이는 한글 표시명이다.
 */
enum class FeatureFlag(val code: String, val label: String) {
    /** 제품 클레임 등록 (POST /api/v1/mobile/claims). */
    PRODUCT_CLAIM("PRODUCT_CLAIM", "제품 클레임 등록"),

    /** 물류 클레임 등록 (POST /api/v1/mobile/suggestions, category=LOGISTICS_CLAIM 포함). */
    LOGISTICS_CLAIM("LOGISTICS_CLAIM", "물류 클레임 등록"),

    /** 주문 등록 (POST /api/v1/mobile/order-requests). */
    ORDER_REQUEST("ORDER_REQUEST", "주문 등록"),
    ;

    companion object {
        /** [code] 로 flag 를 찾는다. 없으면 null. */
        fun fromCode(code: String): FeatureFlag? = entries.find { it.code == code }
    }
}
