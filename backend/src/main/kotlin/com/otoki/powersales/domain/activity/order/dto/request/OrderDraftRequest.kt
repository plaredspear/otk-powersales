package com.otoki.powersales.domain.activity.order.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 주문 임시저장 등록 요청 (Spec #596).
 *
 * 정식 등록과 달리 SAP `InventorySearch` / 여신 / 공급제한 검증 없음 — 폼 데이터 단순 보관.
 * `deliveryDate`(납기일) 는 레거시 Heroku `saveTemp` 가 화면 `#DeliveryRequestDate` 값을
 * `tmp_orderdate` 컬럼에 저장·복원했던 것과 정합 — `tmp_order.order_date` 로 보관한다.
 * 임시저장은 정식 등록의 납기일 필수/유효성 검증을 거치지 않으므로 nullable.
 *
 * **제품 1건 이상 필수 (레거시 정합)**: 레거시 Heroku `write.jsp` 의 임시저장 버튼은
 * `$('.req-order-list').length > 0` 일 때만 활성이라 품목 0건 임시저장은 도달할 수 없었다
 * (서버 `saveTemp` 자체는 무검증이었을 뿐). 거래처만 선택한 빈 임시저장은 복원해도 얻을 것이
 * 없으므로 화면·서버 양쪽에서 막는다.
 */
data class OrderDraftRequest(
    /**
     * 거래처 — **선택 항목** (레거시 정합).
     *
     * 레거시 Heroku `write.jsp` 의 임시저장 버튼은 제품 1건 이상이면 활성이고, 클릭 시
     * 거래처/납기일을 검증하지 않은 채 `tmpAccountcode` 를 빈 값 그대로 보냈다
     * (거래처·납기일 필수 검증은 승인요청 경로에만 있다). 작성 중 상태를 그대로 보관하는
     * 임시저장의 성격과도 맞아 필수 강제를 폐기했다.
     */
    @field:Min(value = 1, message = "accountId 는 1 이상이어야 합니다")
    val accountId: Long? = null,

    /** 납기일(ISO `yyyy-MM-dd`). 임시저장은 검증 없이 그대로 보관. */
    val deliveryDate: LocalDate? = null,

    @field:Min(value = 0, message = "totalAmount 는 0 이상이어야 합니다")
    val totalAmount: Long,

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

    // 수량 0 허용 (레거시 정합) — 제품만 추가하고 수량 미입력 상태로도 임시저장 가능.
    // quantity > 0 강제는 정식 등록(승인요청) 경로에서만 수행한다.
    @field:DecimalMin(value = "0", inclusive = true, message = "quantity 는 0 이상이어야 합니다")
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
