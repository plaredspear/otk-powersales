package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.ClaimDraft
import org.springframework.data.jpa.repository.JpaRepository

interface ClaimDraftRepository : JpaRepository<ClaimDraft, Long> {

    fun findByEmployeeId(employeeId: Long): ClaimDraft?
}
