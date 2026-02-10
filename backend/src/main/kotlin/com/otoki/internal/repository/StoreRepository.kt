package com.otoki.internal.repository

import com.otoki.internal.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 거래처 마스터 Repository
 */
interface StoreRepository : JpaRepository<Store, Long> {

    /**
     * 거래처 코드로 조회
     */
    fun findByStoreCode(storeCode: String): Store?

    /**
     * 거래처 ID 목록으로 일괄 조회
     */
    fun findByIdIn(ids: List<Long>): List<Store>
}
