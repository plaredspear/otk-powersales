package com.otoki.internal.admin.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class PromotionConfirmResponse(
    val promotionId: Long,
    val totalEmployees: Int,
    @JsonProperty("upserted_schedules")
    val upsertedTeamMemberSchedules: Int
)
