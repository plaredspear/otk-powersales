package com.otoki.powersales.platform.common.service

/**
 * 내 거래처 조회 범위 — 레거시 화면 유형별 거래처 조회 기준 차이를 표현한다.
 *
 * 레거시는 화면(JSP)마다 거래처 조회 분기가 달랐다:
 * - [SALES] : 매출 계열(POS/전산/월매출, 레거시 `posMain`/`abcMain`/`monthList`). 부서장(AccountViewAll)이면
 *   일정이 잡힌 전체 거래처를 노출(`selectAllAccount`).
 * - [FIELD] : 현장 활동 계열(판촉/점검/유통기한/클레임, 레거시 `eventList`/`chkWrite`/`expirationWrite`/`claim`).
 *   부서장 전체조회 분기가 없으며 부서장도 여사원과 동일 경로로 처리된다.
 * - [ORDER] : 주문 작성 계열(레거시 `order/my/write.jsp` → `accountSelectList` with `order=order`).
 *   여사원/yang 예외 경로에 한해 (1) 진열 일정(`selectDisplayMyAccount`) union 추가,
 *   (2) 주문가능 거래처유형(`abctypecode__c IN (...)`) 필터를 추가한다.
 *   일반 조장(`teamleaderAccList`)은 레거시에서 abctype 필터가 주석 처리되어 FIELD 와 동일하다.
 * - [ORDER_WRITE] : 주문서 **작성** 화면 전용 (`scope=order` + `purpose=write`).
 *   [ORDER] 와 동일하되, 여사원/yang 예외 경로의 거래처
 *   후보를 **확정 + 오늘 유효한 진열마스터**로 한정한다
 *   (`FeatureFlag.ORDER_ACCOUNT_DISPLAY_SCHEDULE_ONLY` 활성 시. 비활성이면 [ORDER] 와 동일 동작).
 *   주문 조회 필터는 진열 종료 거래처로도 과거 주문을 검색해야 하므로 [ORDER] 를 그대로 쓴다.
 *
 * 여사원/조장 경로는 SALES/FIELD 두 유형 모두 동일하다(team only + 조장 branchCode).
 */
enum class MyAccountScope {
    SALES,
    FIELD,
    ORDER,
    ORDER_WRITE;

    /** 주문 계열 여부 — 주문가능 거래처유형(abctypecode) 필터와 진열 일정 union 의 적용 조건. */
    val isOrder: Boolean
        get() = this == ORDER || this == ORDER_WRITE

    companion object {
        /**
         * 요청 파라미터 → scope 해석.
         *
         * [ORDER_WRITE] 는 `scope=order` + `purpose=write` 조합으로 표현한다. `scope=order_write`
         * 라는 새 값으로 만들지 않는 이유는 **모바일 앱 하위/상위 호환** 때문이다 — 신규 앱이
         * 구버전(롤백된) 서버에 붙었을 때 모르는 scope 값은 [FIELD] 로 떨어져 주문가능 유형 필터와
         * 진열 union 이 통째로 빠지지만, 모르는 **파라미터**는 무시되므로 `purpose` 방식은
         * [ORDER](= 이전 동작)로 안전하게 폴백한다.
         *
         * `scope=order_write` 문자열도 계속 받아준다(호출 측이 한 값으로 보내도 동작하도록).
         */
        fun from(raw: String?, purpose: String? = null): MyAccountScope {
            val scope = when (raw?.lowercase()) {
                "sales" -> SALES
                "order" -> ORDER
                "order_write" -> ORDER_WRITE
                else -> FIELD
            }
            return if (scope == ORDER && purpose?.lowercase() == PURPOSE_WRITE) ORDER_WRITE else scope
        }

        /** 주문서 **작성** 화면임을 알리는 `purpose` 값. */
        private const val PURPOSE_WRITE = "write"
    }
}
