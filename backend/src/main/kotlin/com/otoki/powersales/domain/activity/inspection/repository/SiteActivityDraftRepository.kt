package com.otoki.powersales.domain.activity.inspection.repository

import com.otoki.powersales.domain.activity.inspection.entity.SiteActivityDraft
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 현장점검 임시저장(draft) Repository. 사원 1명당 draft 1건(unique).
 */
interface SiteActivityDraftRepository : JpaRepository<SiteActivityDraft, Long> {
    fun findByEmployeeId(employeeId: Long): SiteActivityDraft?
}
