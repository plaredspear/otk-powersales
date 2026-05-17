package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface PPTHistoryRepositoryCustom {

    fun searchHistories(
        employeeName: String?,
        employeeCode: String?,
        teamType: ProfessionalPromotionTeamType?,
        changedAtFrom: LocalDate?,
        changedAtTo: LocalDate?,
        pageable: Pageable
    ): Page<ProfessionalPromotionTeamHistory>
}
