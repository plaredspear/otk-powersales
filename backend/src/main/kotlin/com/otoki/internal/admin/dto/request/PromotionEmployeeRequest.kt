package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Min
import java.time.LocalDate

data class PromotionEmployeeRequest(
    @field:Size(max = 8, message = "여사원 사번은 최대 8자입니다")
    val employeeId: String? = null,

    val scheduleDate: LocalDate? = null,

    val workStatus: String? = null,

    @field:Size(max = 100, message = "근무유형1은 최대 100자입니다")
    val workType1: String? = null,

    val workType3: String? = null,

    @field:Size(max = 100, message = "근무유형4는 최대 100자입니다")
    val workType4: String? = null,

    @field:Size(max = 100, message = "전문행사조는 최대 100자입니다")
    val professionalPromotionTeam: String? = null,

    @field:Min(value = 0, message = "판매단가는 0 이상이어야 합니다")
    val basePrice: Long? = null,

    @field:Min(value = 0, message = "일일 목표수량은 0 이상이어야 합니다")
    val dailyTargetCount: Int? = null,

    @field:Min(value = 0, message = "목표금액은 0 이상이어야 합니다")
    val targetAmount: Long? = 0,

    @field:Min(value = 0, message = "실적금액은 0 이상이어야 합니다")
    val actualAmount: Long? = 0,

    @field:Min(value = 0, message = "대표품목 매출금액은 0 이상이어야 합니다")
    val primaryProductAmount: Long? = null,

    @field:Min(value = 0, message = "대표품목 판매수량은 0 이상이어야 합니다")
    val primarySalesQuantity: Int? = null,

    @field:Min(value = 0, message = "대표품목 판매단가는 0 이상이어야 합니다")
    val primarySalesPrice: Long? = null,

    @field:Min(value = 0, message = "기타 판매금액은 0 이상이어야 합니다")
    val otherSalesAmount: Long? = null,

    @field:Min(value = 0, message = "기타 판매수량은 0 이상이어야 합니다")
    val otherSalesQuantity: Int? = null,

    @field:Size(max = 255, message = "현장사진 S3 키는 최대 255자입니다")
    val s3ImageUniqueKey: String? = null
)
