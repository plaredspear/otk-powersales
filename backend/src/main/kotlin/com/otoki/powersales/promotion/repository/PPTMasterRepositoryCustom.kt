package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface PPTMasterRepositoryCustom {

    fun searchMasters(
        employeeName: String?,
        employeeCode: String?,
        teamType: ProfessionalPromotionTeamType?,
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
        teamType: ProfessionalPromotionTeamType,
        startDate: LocalDate,
        excludeId: Long? = null
    ): List<ProfessionalPromotionTeamMaster>

    fun findValidMastersByEmployeeId(employeeId: Long, today: LocalDate): List<ProfessionalPromotionTeamMaster>

    fun findSapOutboundTargets(monthFirstDay: LocalDate, monthLastDay: LocalDate): List<ProfessionalPromotionTeamMaster>

    /**
     * 전문행사조 확정 인원 보고서 조회 (Spec #846 — SF Report `new_report_swJ` 이식).
     * `professional_promotion_team_master` ⋈ employee ⋈ account. 전사 (SF scope=organization).
     * 필터: isConfirmed=true (확정), soft-delete 제외.
     * 정렬: branchCode 오름차순.
     */
    fun findConfirmedReport(): List<ProfessionalPromotionTeamMaster>
}

data class PPTMasterSearchResult(
    val master: ProfessionalPromotionTeamMaster,
    val employeeCode: String?,
    val employeeName: String?,
    val accountCode: String?,
    val accountName: String?
)
