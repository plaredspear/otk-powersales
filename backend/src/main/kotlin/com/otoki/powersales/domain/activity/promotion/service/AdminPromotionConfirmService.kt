package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionConfirmResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminPromotionConfirmService(
    private val promotionSchedulesUpsertHelper: PromotionSchedulesUpsertHelper
) {

    @Transactional
    fun confirmPromotion(promotionId: Long): PromotionConfirmResponse {
        return promotionSchedulesUpsertHelper.upsert(promotionId)
    }
}
