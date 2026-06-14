package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpClaimCode
import org.springframework.data.jpa.repository.JpaRepository

interface TmpClaimCodeRepository : JpaRepository<TmpClaimCode, Long>
