package com.otoki.internal.claim.repository

import com.otoki.internal.claim.entity.ClaimPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface AdminClaimPhotoRepository : JpaRepository<ClaimPhoto, Long> {

    fun findByClaimId(claimId: Long): List<ClaimPhoto>
}
