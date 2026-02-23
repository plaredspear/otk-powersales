package com.otoki.internal.repository

import com.otoki.internal.entity.SafetyCheckItem
import com.otoki.internal.entity.SafetyCheckItemId
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyCheckItemRepository : JpaRepository<SafetyCheckItem, SafetyCheckItemId> {

    // Phase2: 기존 V2 필드(categoryId, active, sortOrder, required) 참조 메서드 주석 처리
    // fun findByCategoryIdAndActiveTrueOrderBySortOrderAsc(categoryId: Long): List<SafetyCheckItem>
    // fun findByRequiredTrueAndActiveTrue(): List<SafetyCheckItem>
}
