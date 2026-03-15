package com.otoki.internal.safetycheck.repository

import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.safetycheck.entity.SafetyCheckItemId
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyCheckItemRepository : JpaRepository<SafetyCheckItem, SafetyCheckItemId> {

    fun findByUseYnOrderByQuestionNumAscSeqNumAsc(useYn: String): List<SafetyCheckItem>

    fun countByQuestionNumAndUseYn(questionNum: Int, useYn: String): Long
}
