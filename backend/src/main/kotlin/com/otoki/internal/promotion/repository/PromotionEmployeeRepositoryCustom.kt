package com.otoki.internal.promotion.repository

import java.time.LocalDate

interface PromotionEmployeeRepositoryCustom {

    fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun sumTargetAmountByPromotionId(promotionId: Long): Long

    fun sumActualAmountByPromotionId(promotionId: Long): Long

    fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate?
}
