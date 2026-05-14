package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.enums.ClaimStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface AdminClaimRepositoryCustom {

    fun findClaims(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        status: ClaimStatus?,
        employeeName: String?,
        storeName: String?,
        pageable: Pageable
    ): Page<Claim>
}
