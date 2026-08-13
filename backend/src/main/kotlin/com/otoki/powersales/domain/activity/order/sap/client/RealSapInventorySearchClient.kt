package com.otoki.powersales.domain.activity.order.sap.client

import com.otoki.powersales.domain.activity.order.exception.OrderInvalidRequestException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.external.sap.outbound.sender.InventorySearchSender
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * `SapInventorySearchClient` 의 실제 impl (local 외 전 환경).
 *
 * 레거시 `IF_REST_MOBILE_InventorySearch` 동등 흐름:
 *  1. 거래처 ID → `account.external_key` (SAP 거래처 코드) 매핑
 *  2. SAP `InventorySearch` (SD03070) 1회 호출 — 재고/공급제한/환산수량 조회
 *  3. 자체 `Product` 마스터에서 단가(`standard_unit_price`)/제품명 보강
 *     (레거시 SAP InventorySearch 응답엔 단가가 없음 — 주문 금액 계산은 자체 마스터 단가 기준)
 *
 * local 환경은 [StubSapInventorySearchClient] (`@Profile("local")`) 가 검증을 통과시킨다.
 */
@Component
@Profile("!local")
class RealSapInventorySearchClient(
    private val inventorySearchSender: InventorySearchSender,
    private val accountRepository: AccountRepository,
    private val productRepository: ProductRepository,
) : SapInventorySearchClient {

    private val log = LoggerFactory.getLogger(RealSapInventorySearchClient::class.java)

    override fun search(
        accountId: Long,
        productCodes: List<String>,
        deliveryDate: LocalDate,
    ): Map<String, InventoryInfo> {
        if (productCodes.isEmpty()) return emptyMap()

        val account = accountRepository.findById(accountId)
            .orElseThrow { OrderInvalidRequestException("거래처를 찾을 수 없습니다") }
        val externalKey = account.externalKey?.takeIf { it.isNotBlank() }
            ?: throw OrderInvalidRequestException("거래처 SAP 코드(external_key)가 없습니다")

        // 2. SAP — 재고/공급제한/환산수량 (productCode 키)
        val rawItems = inventorySearchSender.search(externalKey, productCodes, deliveryDate)
        // 동일 productCode 가 2건 이상 오면 associateBy 는 뒤 항목으로 조용히 덮어쓴다 — 결손 행이
        // 뒤에 오면 정상 행이 사라지므로, 단위/환산수량이 채워진 행을 우선 채택하고 흔적을 남긴다.
        val sapItems = rawItems.groupBy { it.productCode }
            .mapValues { (productCode, items) ->
                if (items.size > 1) {
                    log.warn(
                        "sap.inventory.duplicate SAP 응답에 동일 productCode {}건 — externalKey={} productCode={} deliveryDate={}",
                        items.size, externalKey, productCode, deliveryDate,
                    )
                }
                // 기존 동작(associateBy = 마지막 행 채택)을 그대로 유지하되, **마지막 행이 결손일 때만**
                // 온전한 행으로 구제한다. 마지막 행이 정상인 평상시 경로는 종전과 100% 동일하다.
                val last = items.last()
                if (!last.minOrderingUnit.isNullOrBlank() && !last.conversionQuantity.isNullOrBlank()) {
                    last
                } else {
                    items.lastOrNull { !it.minOrderingUnit.isNullOrBlank() && !it.conversionQuantity.isNullOrBlank() }
                        ?: last
                }
            }

        // 3. Product 마스터 — 단가/제품명 보강 (SAP 응답엔 단가 없음)
        val products = productRepository.findByProductCodeIn(productCodes)
            .associateBy { it.productCode }

        return sapItems.values.associate { item ->
            val product = products[item.productCode]
            // 레거시 정합: SAP 응답 MinOrderingUnit 으로 단위 강제 (OrderController.java:548,664).
            // 클라이언트 unit 은 사용하지 않는다.
            val minOrderingUnit = item.minOrderingUnit?.trim().orEmpty()
            val conversionQuantity = item.conversionQuantity.toQuantityOrNull()

            // 운영은 external_api_log 본문 캡처가 꺼져 있어(local/dev 만 캡처) SAP 응답 원문이 남지 않는다.
            // 결손은 곧바로 오주문/차단으로 이어지므로 재현에 필요한 식별자를 여기서 남긴다.
            if (minOrderingUnit.isEmpty() || conversionQuantity == null) {
                log.warn(
                    "sap.inventory.incomplete SAP 발주단위/환산수량 결손 — externalKey={} productCode={} " +
                        "deliveryDate={} minOrderingUnit='{}' conversionQuantity='{}' message='{}'",
                    externalKey, item.productCode, deliveryDate,
                    item.minOrderingUnit, item.conversionQuantity, item.message,
                )
            }

            item.productCode to InventoryInfo(
                productCode = item.productCode,
                productName = product?.name ?: item.productName ?: item.productCode,
                minOrderingUnit = minOrderingUnit,
                conversionQuantity = conversionQuantity,
                // SupplyLimitQTY 누락/공란 = 공급제한 없음 (레거시는 미표시 시 미차단). Int.MAX_VALUE 로 통과.
                supplyLimitQuantity = item.supplyLimitQuantity.toQuantity(default = Int.MAX_VALUE),
                // 레거시 정합: 낱개단가 = 표준단가 + 주세(supertax). 금액 = 낱개단가 × 총 EA.
                unitPrice = product?.let {
                    (it.standardUnitPrice ?: BigDecimal.ZERO) + (it.superTax ?: BigDecimal.ZERO)
                } ?: run {
                    log.warn("Product 마스터 단가 없음 — unitPrice=0 처리 (productCode={})", item.productCode)
                    BigDecimal.ZERO
                },
                message = item.message,
            )
        }
    }

    /**
     * SAP 문자열 수량("1", "1.000", 공란 등) → Int. 파싱 불가 시 [default].
     */
    private fun String?.toQuantity(default: Int): Int {
        val trimmed = this?.trim()
        if (trimmed.isNullOrEmpty()) return default
        return trimmed.toBigDecimalOrNull()?.toInt() ?: default
    }

    /**
     * SAP 환산수량 문자열 → Int. **결손(공란/누락/파싱불가/0 이하)은 null** — 기본값으로 대체하지 않는다
     * ([InventoryInfo.conversionQuantity] 주석의 과다주문 사고 참조).
     */
    private fun String?.toQuantityOrNull(): Int? =
        this?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()?.toInt()?.takeIf { it > 0 }
}
