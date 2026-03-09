package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.PromotionType
import org.springframework.data.jpa.repository.JpaRepository

interface PromotionTypeRepository : JpaRepository<PromotionType, Long> {

    fun findByIsActiveTrueOrderByDisplayOrderAsc(): List<PromotionType>

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean
}
