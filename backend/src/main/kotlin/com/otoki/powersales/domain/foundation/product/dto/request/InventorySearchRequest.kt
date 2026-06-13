package com.otoki.powersales.domain.foundation.product.dto.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 재고조회 요청 (UC-03/04).
 *
 * - productCodes: 1~50건 제한 (레거시 SF InventorySearchController 동일 정책)
 * - accountId: 거래처 PK (SAP 측 거래처 코드는 백엔드에서 매핑 책임)
 * - deliveryRequestDate: 납기일. 컨트롤러 단에서 "내일 이후" 검증
 */
data class InventorySearchRequest(
    @field:NotNull(message = "거래처를 선택해주세요")
    val accountId: Long?,

    @field:NotEmpty(message = "선택하신 제품이 없습니다. 조회 할 제품을 선택해주세요.")
    @field:Size(max = 50, message = "최대 50건까지만 조회가 가능합니다. 조회 할 제품을 줄여주세요.")
    val productCodes: List<String>?,

    @field:NotNull(message = "납기일을 입력해주세요")
    val deliveryRequestDate: LocalDate?
)

/**
 * 선택 제품 엑셀 다운로드 요청 (UC-05).
 */
data class ProductExportRequest(
    @field:NotEmpty(message = "선택된 제품이 없습니다")
    @field:Size(max = 200, message = "최대 200건까지만 내려받을 수 있습니다")
    val productCodes: List<String>?
)
