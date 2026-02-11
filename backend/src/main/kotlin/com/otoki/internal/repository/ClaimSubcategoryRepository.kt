package com.otoki.internal.repository

import com.otoki.internal.entity.ClaimSubcategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 클레임 종류2 Repository
 */
@Repository
interface ClaimSubcategoryRepository : JpaRepository<ClaimSubcategory, Long> {

    /**
     * 특정 종류1에 속하는 활성 종류2 목록 조회
     * sortOrder 순으로 정렬
     */
    fun findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(categoryId: Long): List<ClaimSubcategory>

    /**
     * 활성 상태인 모든 종류2 목록 조회
     * sortOrder 순으로 정렬
     */
    fun findByIsActiveTrueOrderBySortOrderAsc(): List<ClaimSubcategory>
}
