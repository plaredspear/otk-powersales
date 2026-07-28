package com.otoki.powersales.domain.org.employee.enums

/**
 * 직급별 인원현황 표의 **표준 직위** (SF `DKRetail__Jikwee__c`, 직위명).
 *
 * SF prod 메타상 `Jikwee__c` 는 `type=string`, `length=40` 의 **자유 텍스트**(picklist 아님) 라
 * 값이 강제되지 않는다. 운영 데이터에는 아래 값 외에도 `수습사원` / `대리` / `사원` / `과장` / null 이
 * 섞여 있으므로, 각 그룹의 해당 직위를 고정 노출하고 나머지는 [ETC_LABEL] 한 칸으로 합산한다
 * (지점 간 열 구성을 고정해 비교 가능하게 유지). 단 `수습사원` 은 [normalize] 로 [OSPJ] 에 흡수한다.
 *
 * ## 직무별로 쓰는 직위가 다르다
 *
 * 직위는 직무에 종속된다 — 판촉직은 OSPM/OSPE/OSPJ, OSC직은 OSC 뿐이다. 두 그룹에 전체 직위를
 * 일괄 노출하면 판촉직에 OSC 열이, OSC직에 OSPM/OSPE/OSPJ 열이 항상 0으로 붙는다.
 * 그래서 [forJobCode] 로 그룹별 열 집합을 나눈다.
 *
 * 주의: '판매조장' 그룹은 본 고정 열을 쓰지 않는다 — 지점마다 조장의 직위가 달라(강북3지점 OSPM /
 * 강북4지점 주임) 실제 값을 동적으로 노출한다
 * ([com.otoki.powersales.admin.dto.response.RankGroupCount] 참조).
 */
enum class StaffRank(val code: String) {
    OSPM("OSPM"),
    OSPE("OSPE"),
    OSPJ("OSPJ"),
    OSC("OSC"),
    ;

    companion object {
        /** 판촉직 열 — 직위 3종. */
        val PROMOTION_CODES: List<String> = listOf(OSPM.code, OSPE.code, OSPJ.code)

        /** OSC직 열 — 직위 1종. */
        val OSC_CODES: List<String> = listOf(OSC.code)

        /** 표준 직위 외 값(대리·사원·과장 등)과 null 을 합산하는 열 라벨. */
        const val ETC_LABEL = "기타"

        /**
         * 표준 직위로 흡수하는 비표준 직위명.
         *
         * 수습사원은 판촉직 신입 단계라 별도 열('기타')로 떼지 않고 [OSPJ] 에 합산한다.
         */
        private val ALIASES: Map<String, String> = mapOf("수습사원" to OSPJ.code)

        /**
         * 직위명을 표준 직위로 정규화한다. [ALIASES] 에 없으면 원문 그대로 반환.
         *
         * 인원현황 표의 열 판정은 항상 본 함수를 거친 값으로 한다 — 판촉직·OSC직의 고정 열 합산과
         * 판매조장의 동적 열 라벨이 같은 기준을 쓰게 하기 위함.
         */
        fun normalize(jikwee: String): String = ALIASES[jikwee] ?: jikwee

        /**
         * 직무코드에 해당하는 표준 직위 열 목록.
         * 판촉직 → OSPM/OSPE/OSPJ, OSC직·레이디직 → OSC. 그 외 직무는 빈 목록(전량 [ETC_LABEL] 로 합산).
         */
        fun forJobCode(jobCode: String): List<String> = when (jobCode) {
            FemaleStaffJobCode.PROMOTION.code -> PROMOTION_CODES
            in FemaleStaffJobCode.OSC_CODES -> OSC_CODES
            else -> emptyList()
        }
    }
}
