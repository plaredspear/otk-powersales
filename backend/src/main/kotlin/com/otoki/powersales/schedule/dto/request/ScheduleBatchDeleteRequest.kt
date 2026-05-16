package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class ScheduleBatchDeleteRequest(
    @field:NotEmpty(message = "ids는 1건 이상이어야 합니다")
    @field:Size(max = 200, message = "최대 200건까지 처리 가능합니다")
    val ids: List<Long>
)
