package com.otoki.internal.claim.repository

import com.otoki.internal.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ClaimRepository : JpaRepository<Claim, Long> {

    fun findByEmployeeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        employeeId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Claim>
}
