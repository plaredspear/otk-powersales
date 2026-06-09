package com.otoki.powersales.sales.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

/**
 * ORORA 매출이력 수동 적재 트리거 요청 (Spec #855).
 *
 * `salesMonth` 미지정 시 당월 동적 산출 (Q2 옵션 1).
 */
data class OroraSalesMaterializeRequest(
    @field:Pattern(regexp = "^\\d{6}$", message = "salesMonth 는 YYYYMM 6자리 숫자")
    @Schema(description = "대상 매출월 (YYYYMM). 미지정 시 당월", example = "202605")
    val salesMonth: String? = null,
)
