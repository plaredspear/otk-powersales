package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.Promotion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PromotionRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        promotionTypeId: Long?,
        category: String?,
        startDate: String?,
        endDate: String?,
        branchCodes: List<String>?,
        pageable: Pageable
    ): Page<Promotion>
}
