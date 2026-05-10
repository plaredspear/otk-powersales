package com.otoki.powersales.common.repository

import com.otoki.powersales.common.entity.AgreementWord
import java.time.LocalDate

interface AgreementWordRepositoryCustom {

    /**
     * 활성 약관 + 도래 후보 OR 절 조회 (스펙 #654 / cycle batch).
     *
     * 레거시 매핑: `AgreementWordBatch.cls:5-9` SOQL —
     * `WHERE Active__c = True OR (ActiveDate__c = null AND AfterActiveDate__c = today)`.
     * 신규: 동일 조건에 soft-delete 정합으로 `is_deleted = false` (또는 NULL) 추가.
     */
    fun findActiveOrDueCandidates(today: LocalDate): List<AgreementWord>
}
