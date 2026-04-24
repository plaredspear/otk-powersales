package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.ClaimPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClaimPhotoRepository : JpaRepository<ClaimPhoto, Long> {

    fun findByClaimId(claimId: Long): List<ClaimPhoto>
}
