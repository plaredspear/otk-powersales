package com.otoki.powersales.admin.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NaverGeocodeTestRequest(
    @field:NotBlank(message = "주소는 필수입니다")
    @field:Size(min = 1, max = 200, message = "주소는 1~200자 이내여야 합니다")
    val address: String
)
