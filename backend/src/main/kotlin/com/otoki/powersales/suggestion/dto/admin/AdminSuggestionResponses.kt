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

/**
 * admin 제안 목록 row — SF `plant_claim` List View 15개 컬럼 정합.
 *
 * SF 컬럼 순서: NAME / ActionStatus__c / CREATED_DATE / ClaimDate__c / WERK3_TEXT2__c /
 * LogisticsResponsibility__c / ClaimType__c / DKRetail__Title__c / DKRetail__Description__c /
 * DKRetail__ProductId__c / ProductCategory__c / AccountId__c / OrgName__c /
 * DKRetail__EmployeeId__c / CarNumber__c.
 */
data class AdminSuggestionListItem(
    val id: Long,
    val proposalNumber: String,
    val category: SuggestionCategory,
    val categoryName: String,
    val title: String,
    val content: String,
    val employeeName: String?,
    val employeeCode: String?,
    val orgName: String?,
    val accountName: String?,
    val accountCode: String?,
    val productName: String?,
    val productCode: String?,
    /** SF `ProductCategory__c` formula 동등: 보관조건 ∈ {냉동,냉장,만두} → "냉동/냉장", else "상온". */
    val productCategory: String?,
    val claimType: String?,
    val claimDate: LocalDate?,
    val responsibleLogisticsCenter: String?,
    val logisticsResponsibility: String?,
    val carNumber: String?,
    val actionStatus: SuggestionActionStatus?,
    val actionStatusName: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        /** SF `ProductCategory__c` formula (`StoreCondition__c` 기준) 재현. 제품 미연계 시 null. */
        private val COLD_STORE_CONDITIONS = setOf("냉동", "냉장", "만두")

        private fun productCategory(suggestion: Suggestion): String? {
            val condition = suggestion.product?.storeConditionText?.trim()?.takeIf { it.isNotBlank() }
                ?: return null
            return if (condition in COLD_STORE_CONDITIONS) "냉동/냉장" else "상온"
        }

        fun from(suggestion: Suggestion): AdminSuggestionListItem = AdminSuggestionListItem(
            id = suggestion.id,
            proposalNumber = suggestion.proposalNumber,
            category = suggestion.category,
            categoryName = suggestion.category.displayName,
            title = suggestion.title,
            content = suggestion.content,
            employeeName = suggestion.employee?.name,
            employeeCode = suggestion.employee?.employeeCode,
            orgName = suggestion.employee?.orgName,
            accountName = suggestion.account?.name,
            accountCode = suggestion.account?.externalKey,
            productName = suggestion.product?.name,
            productCode = suggestion.productCode,
            productCategory = productCategory(suggestion),
            claimType = suggestion.claimType,
            claimDate = suggestion.claimDate,
            responsibleLogisticsCenter = suggestion.responsibleLogisticsCenter,
            logisticsResponsibility = suggestion.logisticsResponsibility,
            carNumber = suggestion.carNumber,
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
