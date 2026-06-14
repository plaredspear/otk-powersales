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

    fun buildPayload(
        orderRequest: OrderRequest,
        account: Account,
        employee: Employee,
        products: List<OrderRequestProduct>,
    ): Map<String, Any?> {
        // SAP 의 `SAPAccountCode` 는 SF 레거시 IF_REST_MOBILE_OrderRequestRegist 가 ExternalKey__c lookup 키로 사용 —
        // sfid 식별자 미수용. application sfid 사용 금지 정책 + SAP 인터페이스 정합 모두 만족하려면 externalKey 단일 키.
        val sapAccountCode = account.externalKey
            ?: error("Account ${account.id} 의 externalKey 가 비어있어 SAP 송신 불가 — SAP 는 ExternalKey 로 거래처 lookup")
        return mapOf(
            "SAPAccountCode" to sapAccountCode,
            "OrderDate" to orderRequest.orderDate.toLocalDate().format(YYYYMMDD),
            "DeliveryRequestDate" to orderRequest.deliveryDate.format(YYYYMMDD),
            "EmployeeCode" to employee.employeeCode,
            "RequestNumber" to orderRequest.orderRequestNumber,
            "InterfaceType" to "New",
            "TotalOrderAmount" to orderRequest.totalAmount,
            "reqItemList" to products.map { p ->
                mapOf(
                    "LineNumber" to p.lineNumber.toString(),
                    "ProductCode" to p.productCode,
                    "OrderQuantity" to p.quantityBoxes.toPlainString(),
                    "Unit" to p.unit,
                    "TotalQuantity_Each" to p.quantityPieces.toString(),
                    "TotalQuantity_Box" to p.quantityBoxes.toPlainString(),
                )
            },
        )
    }

    companion object {
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
