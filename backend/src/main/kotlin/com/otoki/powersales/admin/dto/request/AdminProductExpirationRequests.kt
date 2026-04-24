package com.otoki.powersales.admin.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class AdminProductExpirationCreateRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    val employeeCode: String,

    @field:NotBlank(message = "거래처 코드는 필수입니다")
    val accountCode: String,

    @field:NotBlank(message = "제품 코드는 필수입니다")
    val productCode: String,

    @field:NotBlank(message = "유통기한은 필수입니다")
    val expirationDate: String,

    @field:NotBlank(message = "알림일은 필수입니다")
    val alarmDate: String,

    @field:Size(max = 500, message = "설명은 500자 이내여야 합니다")
    val description: String? = null
)

data class AdminProductExpirationUpdateRequest(
    @field:NotBlank(message = "유통기한은 필수입니다")
    val expirationDate: String,

    @field:NotBlank(message = "알림일은 필수입니다")
    val alarmDate: String,

    @field:Size(max = 500, message = "설명은 500자 이내여야 합니다")
    val description: String? = null
)

data class AdminProductExpirationBatchDeleteRequest(
    @field:NotEmpty(message = "삭제 대상 ID 목록은 필수입니다")
    @field:Size(max = 100, message = "최대 100건까지 삭제할 수 있습니다")
    val ids: List<Int>
)
