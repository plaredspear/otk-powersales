package com.otoki.internal.admin.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class PPTMasterCreateRequest(
    @field:NotNull(message = "사원 ID는 필수입니다")
    val employeeId: Long,

    @field:NotNull(message = "거래처 ID는 필수입니다")
    val accountId: Int,

    @field:NotBlank(message = "전문행사조 유형은 필수입니다")
    val teamType: String,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    val isConfirmed: Boolean = false
)

data class PPTMasterUpdateRequest(
    @field:NotNull(message = "사원 ID는 필수입니다")
    val employeeId: Long,

    @field:NotNull(message = "거래처 ID는 필수입니다")
    val accountId: Int,

    @field:NotBlank(message = "전문행사조 유형은 필수입니다")
    val teamType: String,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null,

    val isConfirmed: Boolean = false
)

data class PPTMasterBulkValidateRequest(
    @field:Valid
    @field:Size(min = 1, max = 450, message = "업로드 항목은 1~450건이어야 합니다")
    val items: List<PPTMasterBulkItem>
)

data class PPTMasterBulkItem(
    @field:NotBlank(message = "사번은 필수입니다")
    val employeeCode: String,

    @field:NotBlank(message = "거래처코드는 필수입니다")
    val accountCode: String,

    @field:NotBlank(message = "전문행사조 유형은 필수입니다")
    val teamType: String,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null
)
