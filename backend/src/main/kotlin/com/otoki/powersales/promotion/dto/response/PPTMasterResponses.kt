package com.otoki.powersales.promotion.dto.response

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.repository.PPTMasterSearchResult
import java.time.LocalDate
import java.time.LocalDateTime

data class PPTMasterResponse(
    val id: Long,
    val employeeId: Long,
    val employeeCode: String?,
    val employeeName: String?,
    val accountId: Int,
    val accountCode: String?,
    val accountName: String?,
    val teamType: ProfessionalPromotionTeamType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isConfirmed: Boolean,
    val branchCode: String?,
    val branchName: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(result: PPTMasterSearchResult): PPTMasterResponse {
            val m = result.master
            return PPTMasterResponse(
                id = m.id,
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
                branchName = m.branchName,
                createdAt = m.createdAt,
                updatedAt = m.updatedAt
            )
        }

        fun from(
            master: ProfessionalPromotionTeamMaster,
            employeeCode: String?,
            employeeName: String?,
            accountCode: String?,
            accountName: String?
        ): PPTMasterResponse {
            return PPTMasterResponse(
                id = master.id,
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
                branchName = master.branchName,
                createdAt = master.createdAt,
                updatedAt = master.updatedAt
            )
        }
    }
}

data class PPTMasterHistoryResponse(
    val id: Long,
    val employeeId: Long,
    val employeeName: String?,
    val oldValue: ProfessionalPromotionTeamType?,
    val newValue: ProfessionalPromotionTeamType,
    val changedAt: LocalDateTime
) {
    companion object {
        fun from(history: ProfessionalPromotionTeamHistory, employeeName: String?): PPTMasterHistoryResponse {
            return PPTMasterHistoryResponse(
                id = history.id,
                employeeId = history.employeeId,
                employeeName = employeeName,
                oldValue = history.oldValue,
                newValue = history.newValue,
                changedAt = history.changedAt
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
