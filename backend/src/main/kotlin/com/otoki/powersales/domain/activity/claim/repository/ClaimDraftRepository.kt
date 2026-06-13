package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.ClaimDraft
import org.springframework.data.jpa.repository.JpaRepository

interface ClaimDraftRepository : JpaRepository<ClaimDraft, Long> {

    fun findByEmployeeId(employeeId: Long): ClaimDraft?
}
