package com.otoki.internal.repository

import com.otoki.internal.entity.ClaimCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 클레임 종류1 Repository
 */
@Repository
interface ClaimCategoryRepository : JpaRepository<ClaimCategory, Long> {

    /**
     * 활성 상태인 클레임 종류1 목록 조회
     * sortOrder 순으로 정렬
     */
    fun findByIsActiveTrueOrderBySortOrderAsc(): List<ClaimCategory>
}
