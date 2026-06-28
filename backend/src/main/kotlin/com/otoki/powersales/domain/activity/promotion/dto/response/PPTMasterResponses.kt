package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterSearchResult
import java.time.LocalDate
import java.time.LocalDateTime

data class PPTMasterResponse(
    val id: Long,
    // SF Name (AutoNumber PM{0000000}) — 신규 등록분은 채번(PM + 7자리), 마이그레이션 레코드는 SF 채번값 보유
    val name: String?,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val accountId: Long?,
    val accountCode: String?,
    val accountName: String?,
    val teamType: ProfessionalPromotionTeamType?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isConfirmed: Boolean,
    val branchCode: String?,
    // SF BranchName__c — 사원 소속 지점명(Employee.orgName)
    val branchName: String?,
    // SF ValidConditionData__c 프론트 산출용 raw — 재직상태(재직/휴직/퇴직예정/퇴직) 계산
    val employeeStatus: String?,
    val employeeAppLoginActive: Boolean?,
    val employeeEndDate: LocalDate?,
    // SF AccountType__c — 거래처유형 (Account.Type, 한국어 raw 값)
    val accountType: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(result: PPTMasterSearchResult): PPTMasterResponse {
            val m = result.master
            return PPTMasterResponse(
                id = m.id,
                name = m.name,
                employeeId = m.employeeId,
                employeeCode = result.employeeCode,
                employeeName = result.employeeName,
                accountId = m.accountId,
                accountCode = result.accountCode,
                accountName = result.accountName,
                teamType = m.teamType,
                startDate = m.startDate,
                endDate = m.endDate,
                isConfirmed = m.isConfirmed,
                branchCode = m.branchCode,
                branchName = result.branchName,
                employeeStatus = result.employeeStatus,
                employeeAppLoginActive = result.employeeAppLoginActive,
                employeeEndDate = result.employeeEndDate,
                accountType = result.accountType,
                createdAt = m.createdAt,
                updatedAt = m.updatedAt
            )
        }

        fun from(
            master: ProfessionalPromotionTeamMaster,
            employeeCode: String?,
            employeeName: String?,
            accountCode: String?,
            accountName: String?,
            branchName: String? = null,
            employeeStatus: String? = null,
            employeeAppLoginActive: Boolean? = null,
            employeeEndDate: LocalDate? = null,
            accountType: String? = null
        ): PPTMasterResponse {
            return PPTMasterResponse(
                id = master.id,
                name = master.name,
                employeeId = master.employeeId,
                employeeCode = employeeCode,
                employeeName = employeeName,
                accountId = master.accountId,
                accountCode = accountCode,
                accountName = accountName,
                teamType = master.teamType,
                startDate = master.startDate,
                endDate = master.endDate,
                isConfirmed = master.isConfirmed,
                branchCode = master.branchCode,
                branchName = branchName,
                employeeStatus = employeeStatus,
                employeeAppLoginActive = employeeAppLoginActive,
                employeeEndDate = employeeEndDate,
                accountType = accountType,
                createdAt = master.createdAt,
                updatedAt = master.updatedAt
            )
        }
    }
}

data class PPTMasterHistoryResponse(
    val id: Long,
    val name: String?,
    val employeeId: Long?,
    val employeeName: String?,
    val employeeCode: String?,
    val orgName: String?,
    val oldValue: ProfessionalPromotionTeamType?,
    val newValue: ProfessionalPromotionTeamType?,
    val changedAt: LocalDateTime?,
    // 이력을 유발한 마스터(masterId)의 거래처 — masterId 가 null 인 이력은 두 값 모두 null.
    val accountCode: String? = null,
    val accountName: String? = null
) {
    companion object {
        fun from(
            history: ProfessionalPromotionTeamHistory,
            employeeName: String?,
            employeeCode: String?,
            orgName: String?,
            accountCode: String? = null,
            accountName: String? = null
        ): PPTMasterHistoryResponse {
            return PPTMasterHistoryResponse(
                id = history.id,
                name = history.name,
                employeeId = history.employeeId,
                employeeName = employeeName,
                employeeCode = employeeCode,
                orgName = orgName,
                oldValue = history.oldValue,
                newValue = history.newValue,
                changedAt = history.changedAt,
                accountCode = accountCode,
                accountName = accountName
            )
        }
    }
}

data class PPTMasterListResponse(
    val content: List<PPTMasterResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
)

data class PPTMasterHistoryListResponse(
    val content: List<PPTMasterHistoryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int
)

data class BulkValidationResponse(
    val totalCount: Int,
    val successCount: Int,
    val errorCount: Int,
    val isAllValid: Boolean,
    val results: List<BulkValidationResultItem>
)

data class BulkValidationResultItem(
    val row: Int,
    val valid: Boolean,
    val errorMessage: String?
)

data class BulkConfirmResponse(
    val createdCount: Int
)

data class ConfirmByIdsResponse(
    val confirmedCount: Int,
    val skippedCount: Int
)
