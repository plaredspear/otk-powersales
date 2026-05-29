package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.Claim
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

    fun findByCostCenterCodeAndCreatedAtBetweenOrderByCreatedAtDesc(
        costCenterCode: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Claim>

    /** SAP 인바운드 단건 조회 (Spec #561) */
    fun findByName(name: String): Claim?

    /** SAP 인바운드 일괄 조회 (Spec #561) */
    fun findAllByNameIn(names: Collection<String>): List<Claim>
}
