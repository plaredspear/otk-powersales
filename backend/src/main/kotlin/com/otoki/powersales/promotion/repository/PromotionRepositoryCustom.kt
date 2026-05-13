package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.Promotion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PromotionRepositoryCustom {

    fun findByIdWithRelations(id: Long): Promotion?

    fun searchForAdmin(
        keyword: String?,
        promotionTypeId: Long?,
        startDate: String?,
        endDate: String?,
        branchCodes: List<String>?,
        pageable: Pageable
    ): Page<Promotion>

    /**
     * 모바일 행사 조회: 여사원은 배정된 행사만, 그 외는 같은 지점 행사 전체
     */
    fun searchForMobile(
        employeeId: Long?,
        costCenterCode: String?,
        isWoman: Boolean,
        keyword: String?,
        startDate: String?,
        endDate: String?,
        pageable: Pageable
    ): Page<Promotion>
}
