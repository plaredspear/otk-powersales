package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.Promotion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PromotionRepository : JpaRepository<Promotion, Long>, PromotionRepositoryCustom {

    @Query(value = "SELECT nextval('promotion_number_seq')", nativeQuery = true)
    fun getNextPromotionNumberSeq(): Long
}
