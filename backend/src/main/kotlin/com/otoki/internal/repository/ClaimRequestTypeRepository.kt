package com.otoki.internal.repository

import com.otoki.internal.entity.ClaimRequestType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 요청사항 Repository
 */
@Repository
interface ClaimRequestTypeRepository : JpaRepository<ClaimRequestType, String> {

    /**
     * 활성 상태인 요청사항 목록 조회
     * sortOrder 순으로 정렬
     */
    fun findByIsActiveTrueOrderBySortOrderAsc(): List<ClaimRequestType>
}
