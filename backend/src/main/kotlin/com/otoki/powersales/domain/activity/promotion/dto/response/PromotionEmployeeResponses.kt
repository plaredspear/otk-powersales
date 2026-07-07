package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

data class PromotionEmployeeListResponse(
    val id: Long,
    val name: String?,
    val promotionId: Long,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType1: String?,
    val workType3: String?,
    // 전문행사조(현재) — 사원 마스터의 현재 전문행사조 (SF EmployeeId__r.ProfessionalPromotionTeam__c 동등)
    val currentProfessionalPromotionTeam: String?,
    // 전문행사조(투입당시) — 조원일정의 전문행사조 스냅샷 (SF ScheduleId__r.ProfessionalPromotionTeam__c 동등)
    val professionalPromotionTeam: String?,
    val scheduleId: Long?,
    val promoCloseByTm: Boolean,
    val basePrice: BigDecimal?,
    val dailyTargetCount: BigDecimal?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val primaryProductAmount: BigDecimal?,
    val primarySalesQuantity: BigDecimal?,
    val primarySalesPrice: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val s3ImageUniqueKey: String?,
    // 현장사진 조회용 presigned URL (S3 private/ 저장 → 발급 시 만료). key 없거나 미해소 시 null.
    // 레거시 SF SiteImage__c 수식필드(public URL 조합) 대체 — key 소유/삭제는 s3ImageUniqueKey 로 유지.
    val siteImageUrl: String?
) {
    companion object {
        fun from(
            entity: PromotionEmployee,
            employeeName: String?,
            employeeCode: String? = null,
            siteImageUrl: String? = null
        ): PromotionEmployeeListResponse =
            PromotionEmployeeListResponse(
                id = entity.id,
                name = entity.name,
                promotionId = entity.promotionId!!,
                employeeId = entity.employeeId,
                employeeCode = employeeCode,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus?.displayName,
                workType1 = entity.workType1?.displayName,
                workType3 = entity.workType3?.displayName,
                currentProfessionalPromotionTeam = entity.employee?.professionalPromotionTeam?.displayName,
                professionalPromotionTeam = entity.teamMemberSchedule?.professionalPromotionTeam,
                scheduleId = entity.teamMemberScheduleId,
                promoCloseByTm = entity.promoCloseByTm,
                basePrice = entity.basePrice,
                dailyTargetCount = entity.dailyTargetCount,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount,
                primaryProductAmount = entity.primaryProductAmount,
                primarySalesQuantity = entity.primarySalesQuantity,
                primarySalesPrice = entity.primarySalesPrice,
                otherSalesAmount = entity.otherSalesAmount,
                otherSalesQuantity = entity.otherSalesQuantity,
                s3ImageUniqueKey = entity.s3ImageUniqueKey,
                siteImageUrl = siteImageUrl
            )
    }
}

data class PromotionEmployeeDetailResponse(
    val id: Long,
    val name: String?,
    val promotionId: Long,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType1: String?,
    val workType3: String?,
    // 전문행사조(현재) — 사원 마스터의 현재 전문행사조 (SF EmployeeId__r.ProfessionalPromotionTeam__c 동등)
    val currentProfessionalPromotionTeam: String?,
    // 전문행사조(투입당시) — 조원일정의 전문행사조 스냅샷 (SF ScheduleId__r.ProfessionalPromotionTeam__c 동등)
    val professionalPromotionTeam: String?,
    val scheduleId: Long?,
    val promoCloseByTm: Boolean,
    val basePrice: BigDecimal?,
    val dailyTargetCount: BigDecimal?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val primaryProductAmount: BigDecimal?,
    val primarySalesQuantity: BigDecimal?,
    val primarySalesPrice: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val s3ImageUniqueKey: String?,
    // 현장사진 조회용 presigned URL (S3 private/ 저장 → 발급 시 만료). key 없거나 미해소 시 null.
    val siteImageUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            entity: PromotionEmployee,
            employeeName: String?,
            employeeCode: String? = null,
            siteImageUrl: String? = null
        ): PromotionEmployeeDetailResponse =
            PromotionEmployeeDetailResponse(
                id = entity.id,
                name = entity.name,
                promotionId = entity.promotionId!!,
                employeeId = entity.employeeId,
                employeeCode = employeeCode,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus?.displayName,
                workType1 = entity.workType1?.displayName,
                workType3 = entity.workType3?.displayName,
                currentProfessionalPromotionTeam = entity.employee?.professionalPromotionTeam?.displayName,
                professionalPromotionTeam = entity.teamMemberSchedule?.professionalPromotionTeam,
                scheduleId = entity.teamMemberScheduleId,
                promoCloseByTm = entity.promoCloseByTm,
                basePrice = entity.basePrice,
                dailyTargetCount = entity.dailyTargetCount,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount,
                primaryProductAmount = entity.primaryProductAmount,
                primarySalesQuantity = entity.primarySalesQuantity,
                primarySalesPrice = entity.primarySalesPrice,
                otherSalesAmount = entity.otherSalesAmount,
                otherSalesQuantity = entity.otherSalesQuantity,
                s3ImageUniqueKey = entity.s3ImageUniqueKey,
                siteImageUrl = siteImageUrl,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
    }
}
