package com.otoki.powersales.domain.activity.suggestion.dto.response

import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 제안 단건 상세 응답 DTO (Spec #664 P2-B §2.3).
 */
data class SuggestionResponse(
    val id: Long,
    val proposalNumber: String,
    val category: SuggestionCategory,
    val categoryName: String,
    val title: String,
    val content: String,
    val productCode: String?,
    val sapAccountCode: String?,
    val accountId: Long?,
    val employeeId: Long?,
    val claimType: String?,
    val claimTypeMeasures: String?,
    val claimDate: LocalDate?,
    val carNumber: String?,
    val logisticsResponsibility: String?,
    val receptionLogisticsCenter: String?,
    val responsibleLogisticsCenter: String?,
    val actionStatus: SuggestionActionStatus?,
    val actionNum: String?,
    val actionManager: String?,
    val actionContent: String?,
    val duplicateProposalNum: String?,
    val status: SuggestionStatus,
    val createdAt: LocalDateTime,
    val attachments: List<SuggestionAttachment>
) {
    companion object {
        fun from(suggestion: Suggestion, attachments: List<SuggestionAttachment>): SuggestionResponse =
            SuggestionResponse(
                id = suggestion.id,
                proposalNumber = suggestion.proposalNumber,
                category = suggestion.category,
                categoryName = suggestion.category.displayName,
                title = suggestion.title,
                content = suggestion.content,
                productCode = suggestion.product?.productCode,
                sapAccountCode = suggestion.sapAccountCode,
                accountId = suggestion.account?.id?.toLong(),
                employeeId = suggestion.employee?.id,
                claimType = suggestion.claimType,
                claimTypeMeasures = suggestion.claimTypeMeasures,
                claimDate = suggestion.claimDate,
                carNumber = suggestion.carNumber,
                logisticsResponsibility = suggestion.logisticsResponsibility,
                receptionLogisticsCenter = suggestion.receptionLogisticsCenter,
                responsibleLogisticsCenter = suggestion.responsibleLogisticsCenter,
                actionStatus = suggestion.actionStatus,
                actionNum = suggestion.actionNum,
                actionManager = suggestion.actionManager,
                actionContent = suggestion.actionContent,
                duplicateProposalNum = suggestion.duplicateProposalNum,
                status = suggestion.status,
                createdAt = suggestion.createdAt,
                attachments = attachments
            )
    }
}

data class SuggestionListItem(
    val id: Long,
    val proposalNumber: String,
    val category: SuggestionCategory,
    val categoryName: String,
    val title: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(suggestion: Suggestion): SuggestionListItem =
            SuggestionListItem(
                id = suggestion.id,
                proposalNumber = suggestion.proposalNumber,
                category = suggestion.category,
                categoryName = suggestion.category.displayName,
                title = suggestion.title,
                createdAt = suggestion.createdAt
            )
    }
}
