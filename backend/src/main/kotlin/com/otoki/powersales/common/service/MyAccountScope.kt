package com.otoki.powersales.common.service

/**
 * 내 거래처 조회 범위 — 레거시 화면 유형별 거래처 조회 기준 차이를 표현한다.
 *
 * 레거시는 화면(JSP)마다 거래처 조회 분기가 달랐다:
 * - [SALES] : 매출 계열(POS/전산/월매출, 레거시 `posMain`/`abcMain`/`monthList`). 부서장(AccountViewAll)이면
 *   일정이 잡힌 전체 거래처를 노출(`selectAllAccount`).
 * - [FIELD] : 현장 활동 계열(판촉/점검/유통기한/클레임, 레거시 `eventList`/`chkWrite`/`expirationWrite`/`claim`).
 *   부서장 전체조회 분기가 없으며 부서장도 여사원과 동일 경로로 처리된다.
 *
 * 여사원/조장 경로는 두 유형 모두 동일하다(team only + 조장 branchCode).
 */
enum class MyAccountScope {
    SALES,
    FIELD;

    companion object {
        fun from(raw: String?): MyAccountScope =
            if (raw?.lowercase() == "sales") SALES else FIELD
    }
}
