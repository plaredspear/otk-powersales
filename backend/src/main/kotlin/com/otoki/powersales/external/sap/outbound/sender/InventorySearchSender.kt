package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.domain.activity.order.exception.InventorySapErrorException
import com.otoki.powersales.domain.activity.order.exception.InventorySapHtmlResponseException
import com.otoki.powersales.domain.activity.order.exception.InventorySapUnavailableException
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SAP `InventorySearch` (SD03070) 동기 호출 sender.
 *
 * 레거시 `IF_REST_MOBILE_InventorySearch.cls` 동등 — 요청 라인의 productCode 전체를 거래처코드/납기일과
 * 함께 한 번에 조회한다. 레거시 페이로드 형식을 그대로 따라
 * `{"request": [{"SAPAccountCode": "<external_key>", "ProductCode": "<code>", "DeliveryRequestDate": "yyyyMMdd"}, ...]}`
 * 로 POST 하고, 제품별 재고/공급제한/환산수량을 반환한다.
 *
 * **단가(UnitPrice) 는 SAP InventorySearch 응답에 없다** (레거시도 동일 — 주문 등록 시 클라이언트 총액 신뢰).
 * 신규는 호출자([com.otoki.powersales.domain.activity.order.sap.client.RealSapInventorySearchClient]) 가
 * `Product` 마스터(`standard_unit_price`) 에서 단가를 보강한다.
 *
 * `SapResponseHtmlGuard` 로 인증/프록시 HTML 응답을 차단한다. 캐시 없음(실시간 재고 정확도 우선, 레거시 동등).
 */
@Component
class InventorySearchSender(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(InventorySearchSender::class.java)

    /**
     * SAP `InventorySearch` 호출.
     *
     * @param externalKey SF `Account.ExternalKey__c` ≡ 신규 `account.external_key` ≡ SAP 거래처 코드
     * @param productCodes 조회할 제품 코드 목록 (요청 라인 전체, 중복 제거 후 전달 권장)
     * @param deliveryDate 납기 요청일 (레거시 `DeliveryRequestDate` — `yyyyMMdd` 포맷으로 전송)
     * @return SAP 응답 제품별 항목 목록 (응답에 없는 productCode 는 제외됨)
     */
    fun search(
        externalKey: String,
        productCodes: List<String>,
        deliveryDate: LocalDate,
    ): List<InventorySearchSapItem> {
        if (productCodes.isEmpty()) return emptyList()

        val interfaceId = SapConstants.SAP_INTERFACE_INVENTORY_SEARCH
        val endpointPath = "/$interfaceId"
        val dateStr = deliveryDate.format(YYYYMMDD)
        val requestItems = productCodes.map { code ->
            mapOf(
                "SAPAccountCode" to externalKey,
                "ProductCode" to code,
                "DeliveryRequestDate" to dateStr,
            )
        }
        val payload = mapOf("request" to requestItems)

        val body: String? = try {
            restClient.post()
                .uri(endpointPath)
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)
                .body
        } catch (ex: HttpStatusCodeException) {
            log.warn("SAP InventorySearch HTTP {} interfaceId={}", ex.statusCode, interfaceId)
            throw InventorySapUnavailableException("SAP HTTP ${ex.statusCode.value()}")
        } catch (ex: ResourceAccessException) {
            log.warn("SAP InventorySearch 네트워크 실패 interfaceId={} cause={}", interfaceId, ex.message)
            throw InventorySapUnavailableException("SAP 네트워크 오류")
        }

        if (!SapResponseHtmlGuard.isValid(body)) {
            log.warn("SAP InventorySearch HTML 응답 감지 interfaceId={} bodyHead={}", interfaceId, body?.take(80))
            throw InventorySapHtmlResponseException()
        }
        if (body.isNullOrBlank()) {
            throw InventorySapErrorException("SAP 응답 본문 비어 있음")
        }

        val parsed = try {
            objectMapper.readTree(body)
        } catch (ex: Exception) {
            log.warn("SAP InventorySearch 응답 JSON 파싱 실패 bodyHead={}", body.take(80))
            throw InventorySapErrorException("SAP 응답 JSON 파싱 실패")
        }

        // 레거시 IF_REST_MOBILE_InventorySearch:78 — resultCode == 'S' 만 정상.
        val resultCode = parsed["resultCode"]?.asString()
        val resultMsg = parsed["resutlMsg"]?.asString() ?: parsed["resultMsg"]?.asString()
        if (resultCode != "S") {
            throw InventorySapErrorException(resultMsg)
        }

        val resultNode = parsed["result"] ?: throw InventorySapErrorException("SAP 응답 result 필드 누락")
        return resultNode.mapNotNull { it.toInventoryItem() }
    }

    private fun JsonNode.toInventoryItem(): InventorySearchSapItem? {
        val productCode = this["ProductCode"]?.asString()?.takeIf { it.isNotBlank() } ?: return null
        return InventorySearchSapItem(
            productCode = productCode,
            productName = this["ProductName"]?.asString(),
            stockQuantity = this["StockQTY"]?.asString(),
            dcLimitQuantity = this["DCLimitQTY"]?.asString(),
            supplyLimitQuantity = this["SupplyLimitQTY"]?.asString(),
            supplyCenterName = this["SupplyCenterName"]?.asString(),
            closingTime = this["ClosingTime"]?.asString(),
            message = this["Message"]?.asString(),
            minOrderingUnit = this["MinOrderingUnit"]?.asString(),
            conversionQuantity = this["ConversionQuantity"]?.asString(),
        )
    }

    companion object {
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}

/**
 * SAP `InventorySearch` 응답 제품별 항목 (레거시 `IF_REST_MOBILE_InventorySearch.Item` 1:1).
 * SAP 응답 필드는 모두 문자열이며, 수치 변환은 호출자가 책임진다.
 */
data class InventorySearchSapItem(
    val productCode: String,
    val productName: String?,
    val stockQuantity: String?,
    val dcLimitQuantity: String?,
    val supplyLimitQuantity: String?,
    val supplyCenterName: String?,
    val closingTime: String?,
    val message: String?,
    val minOrderingUnit: String?,
    val conversionQuantity: String?,
)
