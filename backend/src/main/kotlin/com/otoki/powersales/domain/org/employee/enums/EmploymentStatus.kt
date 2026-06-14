package com.otoki.powersales.domain.org.employee.enums

/**
 * Employee.status 의 표준 값 (DK Retail SF 운영 관행 기반).
 *
 * SF prod 메타 `DKRetail__Status__c` 는 `type=string`, `picklistValues=[]` (free-form string) 이므로
 * 강제 변환은 적용하지 않으며 Employee.status 필드 타입은 `String?` 그대로 유지한다.
 * 본 enum 은 Repository 필터·테스트의 비교 / 분기에서 [code] 인용으로만 사용한다.
 *
 * - Spec #669 — `DisplayWorkScheduleRepositoryCustomImpl.findValidForDisplayMasterSapPaged` 의
 *   ValidData '유효' 동치 WHERE 절 (SF formula 풀이) 에서 `ACTIVE.code` / `RESIGNED.code` 비교에 사용.
 * - 기존 production 9곳 + test 30+곳의 한글 하드코딩 일괄 치환은 별도 cleanup spec 책임.
 */
enum class EmploymentStatus(val code: String) {
    ACTIVE("재직"),
    RESIGNED("퇴직"),
    ON_LEAVE("휴직"),
    ;

    companion object {
        fun fromCode(code: String?): EmploymentStatus? =
            entries.firstOrNull { it.code == code }
    }
}
