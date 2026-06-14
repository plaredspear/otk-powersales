package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.DailySalesDraft
import org.springframework.data.jpa.repository.JpaRepository

interface DailySalesDraftRepository : JpaRepository<DailySalesDraft, Long> {

    fun findByPromotionEmployeeId(promotionEmployeeId: Long): DailySalesDraft?
}
