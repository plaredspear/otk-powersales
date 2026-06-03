package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionProductRepository : JpaRepository<PromotionProduct, Long>, PromotionProductRepositoryCustom {

    fun findByPromotionId(promotionId: Long): PromotionProduct?


    // SF AutoNumber `PS{00000000}` 동등 — V208 신규 sequence.
    // Native query 라 hibernate.default_schema 가 적용되지 않으므로 schema prefix 명시.
    @Query(value = "SELECT nextval('powersales.promotion_product_name_seq')", nativeQuery = true)
    fun getNextNameSeq(): Long
}
