package com.otoki.powersales.draft.repository

import com.otoki.powersales.draft.entity.TmpClaimCode
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimCodeRepository : JpaRepository<TmpClaimCode, Long>
