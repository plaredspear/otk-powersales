package com.otoki.internal.repository

import com.otoki.internal.entity.SafetyCheckItem
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyCheckItemRepository : JpaRepository<SafetyCheckItem, Long> {

    fun findByCategoryIdAndActiveTrueOrderBySortOrderAsc(categoryId: Long): List<SafetyCheckItem>

    fun findByRequiredTrueAndActiveTrue(): List<SafetyCheckItem>
}
