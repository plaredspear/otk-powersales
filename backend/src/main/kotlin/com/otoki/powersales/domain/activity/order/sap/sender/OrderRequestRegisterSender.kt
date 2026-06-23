package com.otoki.powersales.domain.activity.order.sap.sender

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbox.SapInterfaceRegistry
import com.otoki.powersales.external.sap.outbox.SapOutbox
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * 주문 등록 SAP outbound sender (Spec #592).
 *
 * **직접 SAP 호출 안 함** — `sap_outbox` 적재만 책임. 실제 송신은 [com.otoki.powersales.external.sap.outbox.SapOutboxBatchService] 가 수행.
 *
 * 페이로드 형식: 레거시 `IF_REST_SAP_OrderRequestRegist` 동등 (spec.md §2.3).
 *
 * sap-integration.md §11 sender 컨벤션 준수: 본 컴포넌트는 도메인 트랜잭션 내에서
 * outbox row 를 적재만 한다 (등록 트랜잭션과 단일 트랜잭션 일관성).
 */
@Component
class OrderRequestRegisterSender(
    private val outboxRepository: SapOutboxRepository,
    private val interfaceRegistry: SapInterfaceRegistry,
    private val objectMapper: ObjectMapper,
) {

    @PostConstruct
    fun register() {
        interfaceRegistry.register(
            interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST,
            endpointPath = "/${SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST}",
        )
    }

    /**
     * `sap_outbox` 에 송신 row 를 적재한다 (트랜잭션은 호출자 책임).
     */
    fun enqueue(orderRequest: OrderRequest, products: List<OrderRequestProduct>): SapOutbox {
        val payload = buildPayload(orderRequest, orderRequest.account!!, orderRequest.employee!!, products)
        val outbox = SapOutbox(
            domainType = SapConstants.SAP_DOMAIN_ORDER_REQUEST_REGISTER,
            aggregateId = orderRequest.id,
            interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST,
            payload = objectMapper.writeValueAsString(payload),
        )
        return outboxRepository.save(outbox)
    }

    /**
     * 레거시 `IF_Util.registOrder` (IF_Util.cls:154-235) 의 SAP SD03050 payload 와 동등하게 구성한다.
     *
     * payload 구조 (레거시 `REQUEST_List_header` / `REQUEST_List_item` 2-level wrapper, IF_Util.cls:80-92):
     * ```
     * {
     *   "REQUEST_List_header": { RequestNumber, SAPAccountCode, OrderDate, DeliveryRequestDate },
     *   "REQUEST_List_item":  [ { LineNumber, ProductCode, TotalQuantity, Unit } ]
     * }
     * ```
     * header/item 모두 위 4필드만 송신한다 — `EmployeeCode` / `InterfaceType` / `TotalOrderAmount`
     * (header), `OrderQuantity` / `TotalQuantity_Each` / `TotalQuantity_Box` (item) 는 레거시
     * SAP payload 에 존재하지 않으므로 보내지 않는다 (inbound 적재 전용 필드).
     */
    fun buildPayload(
        orderRequest: OrderRequest,
        account: Account,
        @Suppress("UNUSED_PARAMETER") employee: Employee,
        products: List<OrderRequestProduct>,
    ): Map<String, Any?> {
        // SAP 의 `SAPAccountCode` 는 SF 레거시 IF_Util.registOrder 가 ExternalKey__c lookup 키로 사용 (IF_Util.cls:165) —
        // sfid 식별자 미수용. application sfid 사용 금지 정책 + SAP 인터페이스 정합 모두 만족하려면 externalKey 단일 키.
        val sapAccountCode = account.externalKey
            ?: error("Account ${account.id} 의 externalKey 가 비어있어 SAP 송신 불가 — SAP 는 ExternalKey 로 거래처 lookup")
        return mapOf(
            "REQUEST_List_header" to mapOf(
                "RequestNumber" to orderRequest.orderRequestNumber,
                "SAPAccountCode" to sapAccountCode,
                // 레거시 `yyyyMMdd HHmm` (날짜·시각 사이 공백 1칸, IF_Util.cls:169) 동등.
                // orderDate / deliveryDate 는 SF nillable=true 정합으로 nullable —
                // 신규 앱 생성 주문은 항상 값이 있으나 마이그 SF NULL row 보존 위해 안전 호출 + 빈문자열 fallback.
                "OrderDate" to (orderRequest.orderDate?.format(YYYYMMDD_HHMM) ?: ""),
                "DeliveryRequestDate" to (orderRequest.deliveryDate?.format(YYYYMMDD) ?: ""),
            ),
            "REQUEST_List_item" to products.map { p ->
                mapOf(
                    "LineNumber" to p.lineNumber,
                    "ProductCode" to p.productCode,
                    // 레거시 SAP `TotalQuantity` = `DKRetail__TotalCount__c`, inbound 적재 시
                    // 모바일 `OrderQuantity` 값이 들어감 (IF_REST_MOBILE_OrderRequestRegist.cls:101).
                    // `OrderQuantity` 는 "주문 단위 수량" — BOX 주문이면 박스수량(quantityBoxes),
                    // EA 주문이면 낱개수량(quantityPieces). 단위 무관하게 quantityBoxes 를 보내면
                    // EA 주문(quantityBoxes=0) 시 TotalQuantity=0 으로 송신되어 SAP 에 수량 누락.
                    "TotalQuantity" to if (p.unit == UNIT_BOX) p.quantityBoxes else p.quantityPieces,
                    "Unit" to p.unit,
                )
            },
        )
    }

    companion object {
        private const val UNIT_BOX = "BOX"
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val YYYYMMDD_HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmm")
    }
}
