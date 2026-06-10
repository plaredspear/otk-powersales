package com.otoki.powersales.promotion.dto.response

import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import java.time.LocalDate

data class MobilePromotionListResponse(
    val content: List<MobilePromotionListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class MobilePromotionListItem(
    val id: Long,
    val promotionNumber: String,
    /**
     * 행사명. SF formula `DKRetail__PromotionName__c`
     * (`TEXT(DKRetail__ProductType__c) + '(' + DKRetail__PrimaryProductId__r.Name + ')'`) 동등 파생값.
     * 예: `냉장/냉동(새우깡)`. 레거시 `promotion/event/list.jsp` 카드 1행 정합.
     */
    val promotionName: String?,
    val promotionType: String?,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val standLocation: String?,
    val isClosed: Boolean,
    val myScheduleDate: LocalDate?
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            primaryProductName: String?,
            myScheduleDate: LocalDate?
        ): MobilePromotionListItem = MobilePromotionListItem(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = buildPromotionName(promotion.productType?.displayName, primaryProductName),
            promotionType = promotion.promotionType?.displayName,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            standLocation = promotion.standLocation?.displayName,
            isClosed = promotion.isClosed,
            myScheduleDate = myScheduleDate
        )

        /**
         * SF formula `DKRetail__PromotionName__c` 재현:
         * `TEXT(ProductType) + '(' + PrimaryProduct.Name + ')'`.
         * 두 입력이 모두 비면 null(레거시 빈 `()` 표시 회피).
         */
        private fun buildPromotionName(productType: String?, primaryProductName: String?): String? {
            val type = productType.orEmpty()
            val product = primaryProductName.orEmpty()
            if (type.isEmpty() && product.isEmpty()) return null
            return "$type($product)"
        }
    }
}

data class MobilePromotionDetailResponse(
    val id: Long,
    val promotionNumber: String,
    val promotionType: String?,
    val accountId: Long,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val primaryProductName: String?,
    val otherProduct: String?,
    val message: String?,
    val standLocation: String?,
    val productType: String?,
    val isClosed: Boolean,
    val remark: String?,
    val employees: List<MobilePromotionEmployeeItem>
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            primaryProductName: String?,
            employees: List<MobilePromotionEmployeeItem>
        ): MobilePromotionDetailResponse = MobilePromotionDetailResponse(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionType = promotion.promotionType?.displayName,
            accountId = promotion.account!!.id,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            primaryProductName = primaryProductName,
            otherProduct = promotion.otherProduct,
            message = promotion.message,
            standLocation = promotion.standLocation?.displayName,
            productType = promotion.productType?.displayName,
            isClosed = promotion.isClosed,
            remark = promotion.remark,
            employees = employees
        )
    }
}

/**
 * 홈 "행사매출 등록" → 일 매출 등록 진입화면의 "담당 행사 선택" 목록 항목.
 * 레거시 Heroku `eventlistapi` 응답(오늘 담당 행사) 동등.
 * [id] 는 일매출 마감 폼 진입 키(promotionEmployeeId).
 */
data class MyPromotionAssignmentItem(
    val id: Long,
    val promotionId: Long,
    val promotionNumber: String,
    val promotionType: String?,
    val accountName: String?,
    val scheduleDate: LocalDate?,
    val standLocation: String?,
    /** 여사원 일매출 마감 완료 여부 (promoCloseByTm). */
    val isClosed: Boolean
) {
    companion object {
        fun from(
            entity: PromotionEmployee,
            accountName: String?
        ): MyPromotionAssignmentItem {
            val promotion = entity.promotion!!
            return MyPromotionAssignmentItem(
                id = entity.id,
                promotionId = promotion.id,
                promotionNumber = promotion.promotionNumber,
                promotionType = promotion.promotionType?.displayName,
                accountName = accountName,
                scheduleDate = entity.scheduleDate,
                standLocation = promotion.standLocation?.displayName,
                isClosed = entity.promoCloseByTm
            )
        }
    }
}

data class MobilePromotionEmployeeItem(
    val id: Long,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType3: String?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    /** 조회 사용자 본인에게 배정된 행 여부 (일매출 등록 진입점 노출용). */
    val isMine: Boolean,
    /** 여사원 일매출 마감 완료 여부 (promoCloseByTm). */
    val isClosed: Boolean
) {
    companion object {
        fun from(
            entity: PromotionEmployee,
            employeeName: String?,
            currentEmployeeId: Long?
        ): MobilePromotionEmployeeItem =
            MobilePromotionEmployeeItem(
                id = entity.id,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus?.displayName,
                workType3 = entity.workType3?.displayName,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount,
                isMine = currentEmployeeId != null && entity.employeeId == currentEmployeeId,
                isClosed = entity.promoCloseByTm
            )
    }
}
