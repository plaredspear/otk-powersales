package com.otoki.internal.safetycheck.repository

import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyCheckItemRepository : JpaRepository<SafetyCheckItem, Long> {

    fun findByUseYnOrderByQuestionNumAscSeqNumAsc(useYn: String): List<SafetyCheckItem>

    fun countByQuestionNumAndUseYn(questionNum: Int, useYn: String): Long
}
