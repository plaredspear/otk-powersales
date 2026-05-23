package com.otoki.powersales.auth.entity

/**
 * SAP 발령 JobCode + Jikchak 코드 조합 → SF AppAuthority picklist value 매핑.
 *
 * SF 원본 `AppointmentTriggerHanlder.cls:131-191` 정합 규칙:
 *  - JobCode ∈ {A055, A049, A053} AND Jikchak == D0052 → 조장
 *  - JobCode ∈ {A055, A049, A053} AND Jikchak != D0052 → 여사원
 *  - 그 외 JobCode (지점장 / 영업부장 / 사업부장 / 본부장 / 영업사원 직위) → null
 *
 * 변환 키는 **JobCode + Jikchak 코드 조합** — 직위명 raw String 이 아님.
 * 그 외 직위 (사업부장 / 본부장 등) 는 SF 운영에서도 AppAuthority null + Profile.Name 으로만 분기.
 */
object AppAuthorityMapper {
    private val PROMOTION_JOB_CODES: Set<String> = setOf("A049", "A053", "A055")
    private const val LEADER_JIKCHAK = "D0052"

    fun fromSapCodes(jobCode: String?, jikchak: String?): String? {
        if (jobCode == null || jobCode !in PROMOTION_JOB_CODES) return null
        return if (jikchak == LEADER_JIKCHAK) AppAuthority.LEADER else AppAuthority.WOMAN
    }
}
