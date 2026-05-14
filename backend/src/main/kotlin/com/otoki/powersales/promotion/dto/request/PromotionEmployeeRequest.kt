package com.otoki.powersales.promotion.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class PromotionEmployeeRequest(
    @field:Min(value = 1, message = "사원 ID는 1 이상이어야 합니다")
    val employeeId: Long? = null,

    val scheduleDate: LocalDate? = null,

    val workStatus: String? = null,

    @field:Size(max = 100, message = "근무유형1은 최대 100자입니다")
    val workType1: String? = null,

    val workType3: String? = null,

    @field:Min(value = 0, message = "판매단가는 0 이상이어야 합니다")
    val basePrice: Long? = null,

    @field:Min(value = 0, message = "일일 목표수량은 0 이상이어야 합니다")
    val dailyTargetCount: Long? = null,

    @field:Min(value = 0, message = "목표금액은 0 이상이어야 합니다")
    val targetAmount: Long? = 0,

    @field:Min(value = 0, message = "실적금액은 0 이상이어야 합니다")
    val actualAmount: Long? = 0,

    @field:Min(value = 0, message = "대표품목 매출금액은 0 이상이어야 합니다")
    val primaryProductAmount: Long? = null,

    @field:Min(value = 0, message = "대표품목 판매수량은 0 이상이어야 합니다")
    val primarySalesQuantity: Long? = null,

    @field:Min(value = 0, message = "대표품목 판매단가는 0 이상이어야 합니다")
    val primarySalesPrice: Long? = null,

    @field:Min(value = 0, message = "기타 판매금액은 0 이상이어야 합니다")
    val otherSalesAmount: Long? = null,

    @field:Min(value = 0, message = "기타 판매수량은 0 이상이어야 합니다")
    val otherSalesQuantity: Long? = null,

    @field:Size(max = 255, message = "현장사진 S3 키는 최대 255자입니다")
    val s3ImageUniqueKey: String? = null
)
