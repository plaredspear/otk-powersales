package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
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
        branchCodeFilter: List<String>?,
        pageable: Pageable
    ): Page<ProfessionalPromotionTeamHistory>
}
