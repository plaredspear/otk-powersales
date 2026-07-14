package com.otoki.powersales.domain.activity.suggestion.dto.admin

import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionAttachment
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionStatus
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
    val category: SuggestionCategory?,
    val categoryName: String?,
    val title: String?,
    val content: String?,
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
            categoryName = suggestion.category?.displayName,
            title = suggestion.title,
            content = suggestion.content,
            employeeName = suggestion.employee?.name,
            employeeCode = suggestion.employee?.employeeCode,
            orgName = suggestion.employee?.orgName,
            accountName = suggestion.account?.name,
            accountCode = suggestion.account?.externalKey,
            productName = suggestion.product?.name,
            productCode = suggestion.product?.productCode,
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
    val category: SuggestionCategory?,
    val categoryName: String?,
    val title: String?,
    val content: String?,
    val productCode: String?,
    val productName: String?,
    val sapAccountCode: String?,
    val accountId: Long?,
    val accountName: String?,
    val accountCode: String?,
    val employeeId: Long?,
    val employeeName: String?,
    val employeeCode: String?,
    // SF formula 파생 (§6.7 미저장 — join 원천에서 조회 시 계산). SF 상세 화면 표시 항목 정합.
    val jikwee: String?,
    val orgName: String?,
    val employeeCategory: String?,
    val productType: String?,
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
    // SF "OLS 조치사항" 섹션 정합 — 조치번호/조치담당자/조치내용 (Spec #833 도입 필드).
    val actionNum: String?,
    val actionManager: String?,
    val actionContent: String?,
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
                categoryName = suggestion.category?.displayName,
                title = suggestion.title,
                content = suggestion.content,
                productCode = suggestion.product?.productCode,
                productName = suggestion.product?.name,
                sapAccountCode = suggestion.sapAccountCode,
                accountId = suggestion.account?.id?.toLong(),
                accountName = suggestion.account?.name,
                accountCode = suggestion.account?.externalKey,
                employeeId = suggestion.employee?.id,
                employeeName = suggestion.employee?.name,
                employeeCode = suggestion.employee?.employeeCode,
                // SF formula 재현 — Jikwee__c / OrgName__c 는 사원 필드 직결.
                jikwee = suggestion.employee?.jikwee,
                orgName = suggestion.employee?.orgName,
                // SF EmployeeCategory__c formula: JobCode__c ∈ {OSC직, 판촉직, 레이디직} → '여사원', else '영업'.
                employeeCategory = employeeCategoryOf(suggestion.employee?.jobCode),
                // SF ProductCategory__c formula: StoreCondition__c ∈ {냉동, 냉장, 만두} → '냉동/냉장', else '상온'.
                productType = productTypeOf(suggestion.product?.storeConditionText),
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
                actionNum = suggestion.actionNum,
                actionManager = suggestion.actionManager,
                actionContent = suggestion.actionContent,
                duplicateProposalNum = suggestion.duplicateProposalNum,
                status = suggestion.status,
                createdAt = suggestion.createdAt,
                attachments = attachments
            )

        /**
         * SF `EmployeeCategory__c` formula 재현.
         * `IF(JobCode__c ∈ {OSC직, 판촉직, 레이디직}, '여사원', '영업')`.
         * jobCode 가 null 이면 판정 원천이 없으므로 null (SF formula 는 null 사원 참조 시 '영업' 이지만,
         * 신규는 사원 미연결 제안에 영업/여사원을 단정하지 않고 미표시).
         */
        private fun employeeCategoryOf(jobCode: String?): String? {
            if (jobCode == null) return null
            return if (jobCode in FEMALE_STAFF_JOB_CODES) "여사원" else "영업"
        }

        /**
         * SF `ProductCategory__c` formula 재현.
         * `IF(StoreCondition__c ∈ {냉동, 냉장, 만두}, '냉동/냉장', '상온')`.
         * 원천은 제품의 보관조건 텍스트([com.otoki.powersales.domain.foundation.product.entity.Product.storeConditionText],
         * SF `StoreCondition__c` 원본 — 신규 enum(실온/냉장)엔 없는 냉동/만두 값도 보존). null 이면 미표시.
         */
        private fun productTypeOf(storeConditionText: String?): String? {
            if (storeConditionText == null) return null
            return if (storeConditionText in COLD_STORE_CONDITIONS) "냉동/냉장" else "상온"
        }

        /** SF EmployeeCategory__c formula 의 '여사원' 판정 JobCode 집합. */
        private val FEMALE_STAFF_JOB_CODES = setOf("OSC직", "판촉직", "레이디직")

        /** SF ProductCategory__c formula 의 '냉동/냉장' 판정 StoreCondition 집합. */
        private val COLD_STORE_CONDITIONS = setOf("냉동", "냉장", "만두")
    }
}
