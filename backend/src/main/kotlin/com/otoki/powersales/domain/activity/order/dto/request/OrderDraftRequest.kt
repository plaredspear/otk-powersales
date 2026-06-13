package com.otoki.powersales.domain.activity.order.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * 주문 임시저장 등록 요청 (Spec #596).
 *
 * 정식 등록과 달리 SAP `InventorySearch` / 여신 / 공급제한 검증 없음 — 폼 데이터 단순 보관.
 * `deliveryDate` 는 비범위 (Q8 — 레거시 정합) — 송신해도 무시.
 */
data class OrderDraftRequest(
    @field:Min(value = 1, message = "accountId 는 1 이상이어야 합니다")
    val accountId: Long,

    @field:Min(value = 0, message = "totalAmount 는 0 이상이어야 합니다")
    val totalAmount: Long,

    @field:NotEmpty(message = "lines 는 1건 이상이어야 합니다")
    @field:Valid
    val lines: List<OrderDraftLineRequest>,
)

data class OrderDraftLineRequest(
    @field:Min(value = 0, message = "lineNumber 는 0 이상이어야 합니다")
    val lineNumber: Int,

    @field:NotBlank(message = "productCode 는 필수입니다")
    @field:Size(min = 1, max = 20, message = "productCode 는 1~20자 사이여야 합니다")
    val productCode: String,

    @field:NotBlank(message = "unit 은 필수입니다")
    val unit: String,

    @field:DecimalMin(value = "0.01", inclusive = true, message = "quantity 는 0보다 커야 합니다")
    val quantity: BigDecimal,

    @field:Min(value = 0, message = "quantityPieces 는 0 이상이어야 합니다")
    val quantityPieces: Int? = null,

    @field:DecimalMin(value = "0", inclusive = true, message = "quantityBoxes 는 0 이상이어야 합니다")
    val quantityBoxes: BigDecimal? = null,

    @field:DecimalMin(value = "0", inclusive = true, message = "unitPrice 는 0 이상이어야 합니다")
    val unitPrice: BigDecimal? = null,

    @field:DecimalMin(value = "0", inclusive = true, message = "amount 는 0 이상이어야 합니다")
    val amount: BigDecimal? = null,
)
