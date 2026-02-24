package com.otoki.internal.safetycheck.repository

import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.safetycheck.entity.SafetyCheckItemId
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyCheckItemRepository : JpaRepository<SafetyCheckItem, SafetyCheckItemId> {

    // Phase2: 기존 V2 필드(categoryId, active, sortOrder, required) 참조 메서드 주석 처리
    // fun findByCategoryIdAndActiveTrueOrderBySortOrderAsc(categoryId: Long): List<SafetyCheckItem>
    // fun findByRequiredTrueAndActiveTrue(): List<SafetyCheckItem>
}
