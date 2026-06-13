package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository

interface AdminClaimRepository : JpaRepository<Claim, Long>, AdminClaimRepositoryCustom
