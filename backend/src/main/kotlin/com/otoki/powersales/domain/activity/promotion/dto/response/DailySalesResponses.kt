package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.DailySalesDraft
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.foundation.product.entity.Product
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 일매출 마감 폼 조회 응답.
 *
 * draft(임시저장)가 있으면 draft 값이, 없으면 PromotionEmployee 의 현재 값이 prefill 된다.
 * [imageUrl] 은 prefill 기준 이미지의 anonymous 접근 URL (없으면 null).
 */
data class DailySalesFormResponse(
    val promotionEmployeeId: Long,
    val promotionId: Long?,
    /** 행사유형. 레거시 `write.jsp` 행사 필드 `[행사유형]행사명` 의 앞부분. */
    val promotionType: String?,
    /**
     * 행사명. SF formula `DKRetail__PromotionName__c`(`제품온도타입(대표제품명)`) 동등 파생값.
     * 레거시 `write.jsp` 행사 필드 표기(`[시식]상온(오뚜기카레_매운맛100G)`) 정합.
     */
    val promotionName: String?,
    /** 대표제품명. 레거시 `write.jsp` "대표 제품" 영역 1행(`primaryProductNmTxt`). */
    val primaryProductName: String?,
    /** 대표제품코드. 레거시 `write.jsp` "대표 제품" 영역 2행(`primaryProductCdTxt`). */
    val primaryProductCode: String?,
    /** 행사마스터의 기타제품 텍스트. 레거시 `write.jsp` "기타 제품" 영역(`otherProduct`). */
    val otherProduct: String?,
    val scheduleDate: LocalDate?,
    val employeeName: String?,
    val isClosed: Boolean,
    /** 입력/수정 가능 여부 (본인 + 미마감). */
    val editable: Boolean,
    /** 출근 등록 완료 여부 (마감 선행 조건). 레거시 commutelogId 존재 여부에 대응. */
    val attendanceRegistered: Boolean,
    /** 임시저장 값으로 prefill 되었는지 여부. */
    val hasDraft: Boolean,
    val basePrice: BigDecimal?,
    val primarySalesQuantity: BigDecimal?,
    val primarySalesPrice: BigDecimal?,
    val primaryProductAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val description: String?,
    val imageUrl: String?,
) {
    companion object {
        /**
         * draft(임시저장)가 있으면 draft 값으로, 없으면 PromotionEmployee 현재 값으로 prefill 한다.
         * [pe.employee] / [pe.promotion] lazy 접근을 포함하므로 트랜잭션 내부에서 호출해야 한다.
         */
        fun from(
            pe: PromotionEmployee,
            draft: DailySalesDraft?,
            imageUrl: String?,
            primaryProduct: Product?
        ): DailySalesFormResponse = DailySalesFormResponse(
            promotionEmployeeId = pe.id,
            promotionId = pe.promotionId,
            promotionType = pe.promotion?.promotionType?.displayName,
            promotionName = MobilePromotionListItem.buildPromotionName(
                pe.promotion?.productType,
                primaryProduct?.name
            ),
            primaryProductName = primaryProduct?.name,
            primaryProductCode = primaryProduct?.productCode,
            otherProduct = pe.promotion?.otherProduct,
            scheduleDate = pe.scheduleDate,
            employeeName = pe.employee?.name,
            isClosed = pe.promoCloseByTm,
            editable = !pe.promoCloseByTm,
            attendanceRegistered = pe.teamMemberSchedule?.attendanceLog != null,
            hasDraft = draft != null,
            basePrice = draft?.basePrice ?: pe.basePrice,
            primarySalesQuantity = draft?.primarySalesQuantity ?: pe.primarySalesQuantity,
            primarySalesPrice = draft?.primarySalesPrice ?: pe.primarySalesPrice,
            primaryProductAmount = draft?.primaryProductAmount ?: pe.primaryProductAmount,
            otherSalesQuantity = draft?.otherSalesQuantity ?: pe.otherSalesQuantity,
            otherSalesAmount = draft?.otherSalesAmount ?: pe.otherSalesAmount,
            description = draft?.description ?: pe.description,
            imageUrl = imageUrl
        )
    }
}

/**
 * 일매출 마감/임시저장 처리 결과 응답.
 */
data class DailySalesResult(
    val promotionEmployeeId: Long,
    val isClosed: Boolean,
    val actualAmount: Long?,
    val imageUrl: String?,
)
