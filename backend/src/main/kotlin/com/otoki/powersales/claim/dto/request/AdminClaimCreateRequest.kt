package com.otoki.powersales.claim.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * Web admin 클레임 등록 요청 DTO (multipart/form-data) — Spec #829.
 *
 * Mobile 의 [ClaimCreateRequest] 와 거의 동일하나 다음 차이:
 *   - [sapAccountCode] — Account.externalKey (SAP 코드) 직접 입력. mobile 은 accountId (Long FK) 사용.
 *   - [employeeCode] — 운영자가 영업사원 대신 대리 등록하므로 대상 사번 필수.
 *   - 영수증 조건부 필수 정책은 service 가 검증 (purchaseMethod ∈ {B,C} 면 receipt 필수).
 *   - claimType1/2 는 SF picklist value (A/B/C, AA~CF) 그대로 받는다.
 */
data class AdminClaimCreateRequest(
    @field:NotBlank(message = "거래처 SAP 코드는 필수입니다")
    val sapAccountCode: String?,

    @field:NotBlank(message = "제품 코드는 필수입니다")
    val productCode: String?,

    @field:NotBlank(message = "사번은 필수입니다")
    val employeeCode: String?,

    @field:NotBlank(message = "기한 종류는 필수입니다")
    val dateType: String?,

    /** dateType=EXPIRY_DATE 일 때만 입력 (yyyy-MM-dd). */
    val expirationDate: String? = null,

    /** dateType=MANUFACTURE_DATE 일 때만 입력 (yyyy-MM-dd). */
    val manufacturingDate: String? = null,

    @field:NotBlank(message = "발생일자는 필수입니다")
    val claimDate: String?,

    @field:NotBlank(message = "클레임 대분류는 필수입니다")
    val claimType1: String?,

    @field:NotBlank(message = "클레임 소분류는 필수입니다")
    val claimType2: String?,

    @field:NotNull(message = "불량 수량은 필수입니다")
    @field:Positive(message = "불량 수량은 양수여야 합니다")
    val quantity: BigDecimal?,

    @field:NotBlank(message = "불량 내역은 필수입니다")
    @field:Size(max = 4000, message = "불량 내역은 최대 4000자입니다")
    val description: String?,

    /** SF PurchaseMethod value (A/B/C). 입력 시 amount + receipt 필수 (B/C 한정). */
    val purchaseMethod: String? = null,

    @field:Positive(message = "구매 금액은 양수여야 합니다")
    val amount: BigDecimal? = null,

    /** SF RequestType (multipicklist) — 다중 선택은 ";" 구분. 최대 4개. */
    val requestType: String? = null,
)
