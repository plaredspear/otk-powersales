package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.dto.request.InventorySearchRequest
import com.otoki.powersales.domain.foundation.product.dto.response.InventorySearchResponse
import com.otoki.powersales.domain.foundation.product.dto.response.InventorySearchResultItem
import com.otoki.powersales.domain.foundation.product.exception.InvalidSearchParameterException
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.activity.order.sap.client.SapInventorySearchClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 재고조회 — UC-03 (목록 일괄) + UC-04 (단건 Quick Action) 공용 서비스.
 *
 * 레거시 InventorySearchController 와 동일 정책:
 * - 1~50건 제한 (request DTO `@Size` 로 0/51+ 사전 차단)
 * - 납기일 = 내일 이후 (본 service 에서 검증)
 * - 거래처는 활성 (is_deleted ≠ true) 만 허용
 *
 * SAP 호출은 `SapInventorySearchClient` 위임 (dev: Stub, prod: 후속 스펙 예정 — 본 분기 외).
 */
@Service
@Transactional(readOnly = true)
class AdminProductInventoryService(
    private val productRepository: ProductRepository,
    private val accountRepository: AccountRepository,
    private val sapInventorySearchClient: SapInventorySearchClient
) {

    fun searchInventory(request: InventorySearchRequest): InventorySearchResponse {
        val accountId = request.accountId!!
        val productCodes = request.productCodes!!
        val deliveryRequestDate = request.deliveryRequestDate!!

        validateDeliveryDate(deliveryRequestDate)
        validateAccount(accountId)

        val products = productRepository.findByProductCodeIn(productCodes)
            .associateBy { it.productCode!! }

        val sapResponse = sapInventorySearchClient.search(
            accountId = accountId.toLong(),
            productCodes = productCodes
        )

        val results = productCodes.map { code ->
            val product = products[code]
            val info = sapResponse[code]
            InventorySearchResultItem(
                productCode = code,
                productName = product?.name ?: info?.productName,
                unit = product?.unit,
                conversionQuantity = info?.conversionQuantity ?: 0,
                supplyLimitQuantity = info?.supplyLimitQuantity ?: 0,
                unitPrice = info?.unitPrice ?: BigDecimal.ZERO,
                message = if (info == null) "SAP 응답에 누락된 제품입니다" else null
            )
        }

        return InventorySearchResponse(results = results)
    }

    private fun validateDeliveryDate(date: LocalDate) {
        val tomorrow = LocalDate.now().plusDays(1)
        if (date.isBefore(tomorrow)) {
            throw InvalidSearchParameterException("납기일은 내일 이후만 선택할 수 있습니다")
        }
    }

    private fun validateAccount(accountId: Long) {
        accountRepository.findById(accountId).orElseThrow {
            InvalidSearchParameterException("거래처를 찾을 수 없습니다: $accountId")
        }
    }
}
