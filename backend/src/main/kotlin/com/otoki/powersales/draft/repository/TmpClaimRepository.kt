package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpClaim
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimRepository : JpaRepository<TmpClaim, Long>
