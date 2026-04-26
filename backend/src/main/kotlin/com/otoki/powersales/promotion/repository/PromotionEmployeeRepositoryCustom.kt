package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionEmployee
import java.time.LocalDate

interface PromotionEmployeeRepositoryCustom {

    fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee>

    fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun sumTargetAmountByPromotionId(promotionId: Long): Long

    fun sumActualAmountByPromotionId(promotionId: Long): Long

    fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate?
}
