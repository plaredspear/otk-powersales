/*
package com.otoki.internal.claim.repository

import com.otoki.internal.claim.entity.ClaimPurchaseMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/ **
 * 구매 방법 Repository
 * /
@Repository
interface ClaimPurchaseMethodRepository : JpaRepository<ClaimPurchaseMethod, String> {

    / **
     * 활성 상태인 구매 방법 목록 조회
     * sortOrder 순으로 정렬
     * /
    fun findByIsActiveTrueOrderBySortOrderAsc(): List<ClaimPurchaseMethod>
}
*/
