package com.otoki.powersales.suggestion.dto.admin

import com.otoki.powersales.suggestion.dto.response.SuggestionAttachment
import com.otoki.powersales.suggestion.entity.Suggestion
import com.otoki.powersales.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.suggestion.entity.SuggestionCategory
import com.otoki.powersales.suggestion.entity.SuggestionStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * admin 제안 목록 응답 (Spec #830 P1-B §2.7).
 */
data class AdminSuggestionListResponse(
    val content: List<AdminSuggestionListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AdminSuggestionListItem(
    val id: Long,
    val proposalNumber: String,
    val category: SuggestionCategory,
    val categoryName: String,
    val title: String,
    val employeeName: String?,
    val employeeCode: String?,
    val accountName: String?,
    val accountCode: String?,
    val productName: String?,
    val productCode: String?,
    val claimType: String?,
    val claimDate: LocalDate?,
    val actionStatus: SuggestionActionStatus?,
    val actionStatusName: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(suggestion: Suggestion): AdminSuggestionListItem = AdminSuggestionListItem(
            id = suggestion.id,
            proposalNumber = suggestion.proposalNumber,
            category = suggestion.category,
            categoryName = suggestion.category.displayName,
            title = suggestion.title,
            employeeName = suggestion.employee?.name,
            employeeCode = suggestion.employee?.employeeCode,
            accountName = suggestion.account?.name,
            accountCode = suggestion.account?.externalKey,
            productName = suggestion.product?.name,
            productCode = suggestion.productCode,
            claimType = suggestion.claimType,
            claimDate = suggestion.claimDate,
            actionStatus = suggestion.actionStatus,
            actionStatusName = suggestion.actionStatus?.displayName,
            createdAt = suggestion.createdAt
        )
    }
}

/**
 * admin 제안 단건 상세 응답 (Spec #830 P1-B §2.7).
 *
 * mobile [SuggestionResponse] 필드 + admin 추가 필드 (사원/거래처/제품 자연어 라벨 + costCenter).
 */
data class AdminSuggestionDetailResponse(
    val id: Long,
    val proposalNumber: String,
    val category: SuggestionCategory,
    val categoryName: String,
    val title: String,
    val content: String,
    val productCode: String?,
    val productName: String?,
    val sapAccountCode: String?,
    val accountId: Long?,
    val accountName: String?,
    val accountCode: String?,
    val employeeId: Long?,
    val employeeName: String?,
    val employeeCode: String?,
    val orgCostCenterCode: String?,
    val claimType: String?,
    val claimTypeMeasures: String?,
    val claimDate: LocalDate?,
    val carNumber: String?,
    val logisticsResponsibility: String?,
    val receptionLogisticsCenter: String?,
    val responsibleLogisticsCenter: String?,
    val actionStatus: SuggestionActionStatus?,
    val actionStatusName: String?,
    val duplicateProposalNum: String?,
    val status: SuggestionStatus,
    val createdAt: LocalDateTime,
    val attachments: List<SuggestionAttachment>
) {
    companion object {
        fun from(suggestion: Suggestion, attachments: List<SuggestionAttachment>): AdminSuggestionDetailResponse =
            AdminSuggestionDetailResponse(
                id = suggestion.id,
                proposalNumber = suggestion.proposalNumber,
                category = suggestion.category,
                categoryName = suggestion.category.displayName,
                title = suggestion.title,
                content = suggestion.content,
                productCode = suggestion.productCode,
                productName = suggestion.product?.name,
                sapAccountCode = suggestion.sapAccountCode,
                accountId = suggestion.account?.id?.toLong(),
                accountName = suggestion.account?.name,
                accountCode = suggestion.account?.externalKey,
                employeeId = suggestion.employee?.id,
                employeeName = suggestion.employee?.name,
                employeeCode = suggestion.employee?.employeeCode,
                orgCostCenterCode = suggestion.orgCostCenterCode,
                claimType = suggestion.claimType,
                claimTypeMeasures = suggestion.claimTypeMeasures,
                claimDate = suggestion.claimDate,
                carNumber = suggestion.carNumber,
                logisticsResponsibility = suggestion.logisticsResponsibility,
                receptionLogisticsCenter = suggestion.receptionLogisticsCenter,
                responsibleLogisticsCenter = suggestion.responsibleLogisticsCenter,
                actionStatus = suggestion.actionStatus,
                actionStatusName = suggestion.actionStatus?.displayName,
                duplicateProposalNum = suggestion.duplicateProposalNum,
                status = suggestion.status,
                createdAt = suggestion.createdAt,
                attachments = attachments
            )
    }
}
