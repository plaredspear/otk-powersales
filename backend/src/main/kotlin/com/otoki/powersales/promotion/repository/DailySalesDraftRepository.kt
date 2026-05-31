package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.DailySalesDraft
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesDraftRepository : JpaRepository<DailySalesDraft, Long> {

    fun findByPromotionEmployeeId(promotionEmployeeId: Long): DailySalesDraft?
}
