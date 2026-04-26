package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.ClaimPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface AdminClaimPhotoRepository : JpaRepository<ClaimPhoto, Long> {

    fun findByClaimId(claimId: Long): List<ClaimPhoto>
}
