package com.otoki.powersales.account.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 관리자 웹 신규 거래처 등록 요청 DTO. (Spec #640)
 */
data class AdminAccountCreateRequest(

    @field:NotBlank(message = "거래처명은 필수입니다.")
    @field:Size(max = 255, message = "거래처명은 255자 이하여야 합니다.")
    val name: String,

    @field:NotBlank(message = "담당 영업사원 사번은 필수입니다.")
    @field:Size(max = 20, message = "담당 영업사원 사번은 20자 이하여야 합니다.")
    val employeeCode: String
)
