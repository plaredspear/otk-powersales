package com.otoki.internal.repository

import com.otoki.internal.entity.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 클레임 Repository
 */
@Repository
interface ClaimRepository : JpaRepository<Claim, Long> {

    /**
     * 사용자별 클레임 목록 조회
     * 최신순으로 정렬
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Claim>

    /**
     * 거래처별 클레임 목록 조회
     * 최신순으로 정렬
     */
    fun findByStoreIdOrderByCreatedAtDesc(storeId: Long): List<Claim>
}
