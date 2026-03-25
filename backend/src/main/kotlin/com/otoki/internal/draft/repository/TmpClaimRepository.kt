package com.otoki.internal.draft.repository

import com.otoki.internal.draft.entity.TmpClaim
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimRepository : JpaRepository<TmpClaim, Long>
