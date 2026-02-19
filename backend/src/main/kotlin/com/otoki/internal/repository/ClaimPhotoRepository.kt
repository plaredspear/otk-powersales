/*
package com.otoki.internal.repository

import com.otoki.internal.entity.ClaimPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/ **
 * 클레임 사진 Repository
 * /
@Repository
interface ClaimPhotoRepository : JpaRepository<ClaimPhoto, Long> {

    / **
     * 클레임별 사진 목록 조회
     * /
    fun findByClaimId(claimId: Long): List<ClaimPhoto>
}
*/
