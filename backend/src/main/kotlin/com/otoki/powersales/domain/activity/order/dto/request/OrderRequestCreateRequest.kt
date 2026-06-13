package com.otoki.powersales.domain.activity.order.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 주문 등록 요청 (Spec #592 §5.2).
 */
data class OrderRequestCreateRequest(
    @field:Size(max = 64, message = "clientRequestId 길이는 64자 이하입니다")
    val clientRequestId: String? = null,

    @field:NotNull
    @field:Min(1, message = "accountId 는 1 이상이어야 합니다")
    val accountId: Long,

    @field:NotNull
    val deliveryDate: LocalDate,

    @field:NotNull
    @field:Min(1, message = "totalAmount 는 1 이상이어야 합니다")
    val totalAmount: Long,

    @field:NotEmpty(message = "lines 는 1건 이상이어야 합니다")
    @field:Valid
    val lines: List<OrderRequestCreateLine>,
)

data class OrderRequestCreateLine(
    @field:NotNull
    @field:Min(0, message = "lineNumber 는 0 이상이어야 합니다")
    val lineNumber: Int,

    @field:NotBlank
    @field:Size(min = 1, max = 20, message = "productCode 길이는 1~20자입니다")
    val productCode: String,

    @field:NotNull
    @field:DecimalMin(value = "0", inclusive = false, message = "quantity 는 양수여야 합니다")
    val quantity: BigDecimal,

    @field:NotBlank
    val unit: String,

    @field:NotNull
    @field:Min(1, message = "quantityPieces 는 1 이상이어야 합니다")
    val quantityPieces: Int,

    @field:NotNull
    @field:DecimalMin(value = "0", inclusive = true, message = "quantityBoxes 는 0 이상이어야 합니다")
    val quantityBoxes: BigDecimal,
)
