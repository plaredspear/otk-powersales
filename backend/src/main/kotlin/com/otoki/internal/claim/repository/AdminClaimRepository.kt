package com.otoki.internal.claim.repository

import com.otoki.internal.claim.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository

interface AdminClaimRepository : JpaRepository<Claim, Long>, AdminClaimRepositoryCustom
