package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface PPTMasterRepositoryCustom {

    fun searchMasters(
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        validOnly: Boolean,
        today: LocalDate,
        pageable: Pageable
    ): Page<PPTMasterSearchResult>

    fun findValidMasters(today: LocalDate): List<ProfessionalPromotionTeamMaster>

    fun findExpiringMasters(today: LocalDate): List<ProfessionalPromotionTeamMaster>

    fun findValidMastersByEmployeeIdAndTeamType(
        employeeId: Long,
        accountId: Int,
        teamType: String,
        startDate: LocalDate,
        excludeId: Long? = null
    ): List<ProfessionalPromotionTeamMaster>

    fun findValidMastersByEmployeeId(employeeId: Long, today: LocalDate): List<ProfessionalPromotionTeamMaster>

    fun findSapOutboundTargets(monthFirstDay: LocalDate, monthLastDay: LocalDate): List<ProfessionalPromotionTeamMaster>
}

data class PPTMasterSearchResult(
    val master: ProfessionalPromotionTeamMaster,
    val employeeCode: String?,
    val employeeName: String?,
    val accountCode: String?,
    val accountName: String?
)
