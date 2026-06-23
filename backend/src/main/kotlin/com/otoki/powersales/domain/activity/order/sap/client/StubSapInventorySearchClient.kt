package com.otoki.powersales.domain.activity.order.sap.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * local 환경 전용 stub. 실제 SAP 호출은 [RealSapInventorySearchClient] (`@Profile("!local")`) 가 담당한다.
 *
 * - local profile 에서만 활성 (`@Profile("local")`)
 * - 그 외 환경(dev/test/staging/prod)에서는 실제 SAP 호출이 동작
 *
 * Stub 동작: 모든 productCode 에 대해 `minOrderingUnit="EA"`, `conversionQuantity=1`,
 * `supplyLimitQuantity=Int.MAX_VALUE`, `unitPrice=0` 반환 — 검증을 모두 통과시킴 (로컬 개발 편의).
 */
@Component
@Profile("local")
class StubSapInventorySearchClient : SapInventorySearchClient {

    private val log = LoggerFactory.getLogger(StubSapInventorySearchClient::class.java)

    override fun search(accountId: Long, productCodes: List<String>, deliveryDate: LocalDate): Map<String, InventoryInfo> {
        log.debug("Stub InventorySearch — accountId={} productCodes={} deliveryDate={}", accountId, productCodes, deliveryDate)
        return productCodes.associateWith { code ->
            InventoryInfo(
                productCode = code,
                productName = "STUB_$code",
                minOrderingUnit = "EA",
                conversionQuantity = 1,
                supplyLimitQuantity = Int.MAX_VALUE,
                unitPrice = BigDecimal.ZERO,
            )
        }
    }
}
