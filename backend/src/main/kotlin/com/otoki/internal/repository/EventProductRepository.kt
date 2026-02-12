package com.otoki.internal.repository

import com.otoki.internal.entity.EventProduct
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 행사 제품 Repository
 */
interface EventProductRepository : JpaRepository<EventProduct, Long> {

    /**
     * 행사별 제품 목록 조회
     */
    fun findByEventId(eventId: String): List<EventProduct>

    /**
     * 행사별 대표 제품 조회
     */
    fun findByEventIdAndIsMainProduct(eventId: String, isMainProduct: Boolean): EventProduct?

    /**
     * 행사별 제품 코드 존재 여부 확인
     */
    fun existsByEventIdAndProductCode(eventId: String, productCode: String): Boolean
}
