package com.otoki.powersales.domain.org.employee.enums

/**
 * 직급별 인원현황 표의 **표준 직위** (SF `DKRetail__Jikwee__c`, 직위명).
 *
 * SF prod 메타상 `Jikwee__c` 는 `type=string`, `length=40` 의 **자유 텍스트**(picklist 아님) 라
 * 값이 강제되지 않는다. 운영 데이터에는 아래 4값 외에도 `수습사원` / `대리` / `사원` / `과장` / null 이
 * 섞여 있으므로, 판촉직·OSC직 열은 본 enum 을 고정 노출하고 나머지는 [ETC_LABEL] 한 칸으로 합산한다
 * (지점 간 열 구성을 고정해 비교 가능하게 유지).
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
        /** 표의 열 순서 — 판촉직(OSPM·OSPE·OSPJ) 다음 OSC직(OSC). */
        val ORDERED_CODES: List<String> = entries.map { it.code }

        /** 표준 직위 외 값(수습사원·대리·사원·과장 등)과 null 을 합산하는 열 라벨. */
        const val ETC_LABEL = "기타"

        fun contains(jikwee: String?): Boolean = jikwee?.trim() in ORDERED_CODES
    }
}
