package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpClaim
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimRepository : JpaRepository<TmpClaim, Long>
