package com.otoki.internal.safetycheck.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class SafetyCheckSubmitRequest(
    @field:NotNull(message = "시작 시각은 필수입니다")
    val startTime: LocalDateTime,

    @field:NotNull(message = "완료 시각은 필수입니다")
    val completeTime: LocalDateTime,

    @field:NotEmpty(message = "장비 항목 응답이 비어있습니다")
    @field:Valid
    val equipments: List<EquipmentAnswer>,

    val precautions: List<String>? = null
) {
    data class EquipmentAnswer(
        @field:NotNull(message = "순번은 필수입니다")
        val seqNum: Int,

        @field:NotNull(message = "응답은 필수입니다")
        val answer: String
    )
}
