package com.otoki.powersales.order.sap.client

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * Spec #592 의 임시 stub. **#594 후속 스펙이 실제 SAP 호출 impl 을 추가하면 본 클래스 제거 예정**.
 *
 * - dev/test profile 에서만 활성 (`@Profile("!prod")`)
 * - prod 환경에선 본 stub 미등록 → #594 머지 전엔 기동 실패 (운영 위험 차단)
 *
 * Stub 동작: 모든 productCode 에 대해 `conversionQuantity=1`, `supplyLimitQuantity=Int.MAX_VALUE`,
 * `unitPrice=0` 반환 — 검증을 모두 통과시킴 (개발 편의 + 통합 테스트 stub).
 */
@Component
@Profile("!prod")
class StubSapInventorySearchClient : SapInventorySearchClient {

    private val log = LoggerFactory.getLogger(StubSapInventorySearchClient::class.java)

    override fun search(accountId: Long, productCodes: List<String>): Map<String, InventoryInfo> {
        log.debug("Stub InventorySearch — accountId={} productCodes={}", accountId, productCodes)
        return productCodes.associateWith { code ->
            InventoryInfo(
                productCode = code,
                productName = "STUB_$code",
                conversionQuantity = 1,
                supplyLimitQuantity = Int.MAX_VALUE,
                unitPrice = BigDecimal.ZERO,
            )
        }
    }
}
