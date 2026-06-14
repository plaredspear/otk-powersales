package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.foundation.account.entity.AccountType
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
        accountId: Long,
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
    val accountName: String?,
    // SF listView 정합 — 사원 소속 지점명(BranchName__c = FullName__r.DKRetail__OrgName__c)
    val branchName: String?,
    // SF 재직상태(ValidConditionData__c) 프론트 산출용 raw 필드 (재직/휴직/퇴직예정/퇴직)
    val employeeStatus: String?,
    val employeeAppLoginActive: Boolean?,
    val employeeEndDate: LocalDate?,
    // SF 거래처유형(AccountType__c = Account.Type)
    val accountType: AccountType?
)
