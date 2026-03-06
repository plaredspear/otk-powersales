package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.TmpClaim
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimRepository : JpaRepository<TmpClaim, Long> {

    fun findByClaimName(claimName: String): TmpClaim?
}
