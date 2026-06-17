package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.DailySalesDraft
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
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
         * [pe.employee] lazy 접근을 포함하므로 트랜잭션 내부에서 호출해야 한다.
         */
        fun from(
            pe: PromotionEmployee,
            draft: DailySalesDraft?,
            imageUrl: String?
        ): DailySalesFormResponse = DailySalesFormResponse(
            promotionEmployeeId = pe.id,
            promotionId = pe.promotionId,
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
