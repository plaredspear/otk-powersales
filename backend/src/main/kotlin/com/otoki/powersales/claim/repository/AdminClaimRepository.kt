package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository

interface AdminClaimRepository : JpaRepository<Claim, Long>, AdminClaimRepositoryCustom
