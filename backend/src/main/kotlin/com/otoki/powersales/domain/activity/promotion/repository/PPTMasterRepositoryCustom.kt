package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface PPTMasterRepositoryCustom {

    /**
     * @param branchCodeFilter 사원(`employee`) 소속 지점(costCenterCode) 스코프 필터.
     *   `null` 이면 전사(필터 없음), 비어있지 않으면 해당 지점들로 제한.
     *   전문행사조 데이터의 `branch_code` 컬럼은 SF `CostCenterCode__c`(dead field) 출처라 비어 있으므로,
     *   지점 가시성은 사원 조인 후 `employee.costCenterCode` 기준으로 평가한다.
     */
    fun searchMasters(
        employeeName: String?,
        employeeCode: String?,
        teamType: ProfessionalPromotionTeamType?,
        branchCodeFilter: List<String>?,
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
     * 전문행사조 마스터 SAP **단건** 테스트 송신용 조회 (admin SAP outbound 테스트 탭).
     * `findSapOutboundTargets` 와 동일하게 employee/account 를 fetchJoin 하여 payload 변환 시
     * LAZY 미초기화(ValidConditionData 등 사원 필드 참조)를 방지한다. batch 의 월 기간/유효 필터는
     * 적용하지 않고 id 로만 특정한다 — 테스트 목적상 임의 마스터 1건을 그대로 송신 payload 로 만들 수 있게 한다.
     * 존재하지 않으면 null.
     */
    fun findByIdForSapOutbound(masterId: Long): ProfessionalPromotionTeamMaster?

    /**
     * 전문행사조 확정 인원 보고서 조회 (Spec #846 — SF Report `new_report_swJ` 이식).
     * `professional_promotion_team_master` ⋈ employee ⋈ account.
     * 필터: isConfirmed=true (확정), soft-delete 제외.
     * 정렬: branchCode 오름차순.
     *
     * @param branchCodeFilter 사원(`employee`) 소속 지점(costCenterCode) 스코프 필터.
     *   `null` 이면 전사(SF scope=organization 동등), 비어있지 않으면 해당 지점들로 제한.
     *   [searchMasters] 와 동일하게 `employee.costCenterCode` 기준으로 평가한다.
     */
    fun findConfirmedReport(branchCodeFilter: List<String>?): List<ProfessionalPromotionTeamMaster>
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
    val accountType: String?
)
