package com.otoki.internal.draft.repository

import com.otoki.internal.draft.entity.TmpClaimCode
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimCodeRepository : JpaRepository<TmpClaimCode, Long>
