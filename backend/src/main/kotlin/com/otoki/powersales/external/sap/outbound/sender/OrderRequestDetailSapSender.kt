package com.otoki.powersales.external.sap.outbound.sender

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

/**
 * SAP `OrderRequestDetail` 동기 호출 sender (Spec #595).
 *
 * 레거시 `IF_REST_MOBILE_OrderRequestDetail.cls:99-176` 동등 — `{"request": {"RequestNumber": "<번호>"}}`
 * 한 단계 감싼 페이로드로 SAP REST Adapter 호출.
 *
 * **단순 try-catch 빈 결과 fallback** — 호출 실패 / HTML / 503 / Timeout / `resultCode != 'S'` 모두 빈 결과로 매핑.
 * 예외를 던지지 않고 `null` 또는 빈 라인 배열을 반환하여 service layer 가 200 OK 유지하도록 한다 (레거시 `cls:172-176` 동등).
 *
 * 캐시 없음 — 상세 조회는 매 요청마다 최신 SAP 처리 상태 정확도 우선 (레거시 매 호출 직호출).
 */
@Component
class OrderRequestDetailSapSender(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(OrderRequestDetailSapSender::class.java)

    /**
     * SAP `OrderRequestDetail` 호출.
     *
     * @param requestNumber `OrderRequest.orderRequestNumber` (SF `Name`)
     * @return SAP 응답 라인 배열. 호출 실패 / SAP 측 오류 시 `null`.
     */
    fun fetchDetail(requestNumber: String): List<SapOrderRequestDetailLine>? {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL
        val endpointPath = "/$interfaceId"
        val payload = mapOf("request" to mapOf("RequestNumber" to requestNumber))

        val body: String? = try {
            restClient.post()
                .uri(endpointPath)
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)
                .body
        } catch (ex: HttpStatusCodeException) {
            log.warn(
                "sap.outbound.detail.failure HTTP={} requestNumber={} interfaceId={}",
                ex.statusCode, requestNumber, interfaceId,
            )
            return null
        } catch (ex: ResourceAccessException) {
            log.warn(
                "sap.outbound.detail.failure 네트워크 실패 requestNumber={} interfaceId={} cause={}",
                requestNumber, interfaceId, ex.message,
            )
            return null
        }

        if (!SapResponseHtmlGuard.isValid(body)) {
            log.warn(
                "sap.outbound.detail.failure HTML 응답 감지 requestNumber={} bodyHead={}",
                requestNumber, body?.take(80),
            )
            return null
        }
        if (body.isNullOrBlank()) {
            log.warn("sap.outbound.detail.failure 응답 본문 비어 있음 requestNumber={}", requestNumber)
            return null
        }

        val parsed = try {
            objectMapper.readTree(body)
        } catch (ex: Exception) {
            log.warn(
                "sap.outbound.detail.failure 응답 JSON 파싱 실패 requestNumber={} bodyHead={}",
                requestNumber, body.take(80),
            )
            return null
        }

        val resultCode = parsed["resultCode"]?.asString()
        if (resultCode != "S") {
            val resultMsg = parsed["resutlMsg"]?.asString() ?: parsed["resultMsg"]?.asString()
            log.warn(
                "sap.outbound.detail.failure resultCode={} requestNumber={} msg={}",
                resultCode, requestNumber, resultMsg,
            )
            return null
        }

        val resultArray = parsed["result"] ?: return emptyList()
        if (!resultArray.isArray) return emptyList()

        return resultArray.values().map(::mapLine)
    }

    private fun mapLine(node: JsonNode): SapOrderRequestDetailLine =
        SapOrderRequestDetailLine(
            lineNumber = node["LineNumber"]?.asString(),
            productCode = node["ProductCode"]?.asString(),
            productName = node["ProductName"]?.asString(),
            lineItemStatus = node["LineItemStatus"]?.asString(),
            totalQuantity = node["TotalQuantity"]?.asString(),
            unit = node["Unit"]?.asString(),
            sapOrderNumber = node["SAPOrderNumber"]?.asString(),
            orderSalesAmount = node["OrderSalesAmount"]?.asString(),
            deliveryRequestDate = node["DeliveryRequestDate"]?.asString(),
            orderDate = node["OrderDate"]?.asString(),
            shippingDriverName = node["ShippingDriverName"]?.asString(),
            shippingVehicle = node["ShippingVehicle"]?.asString(),
            shippingDriverPhone = node["ShippingDriverPhone"]?.asString(),
            shippingScheduleTime = node["ShippingScheduleTime"]?.asString(),
            shippingCompleteTime = node["ShippingCompleteTime"]?.asString(),
            totalQuantityBox = node["TotalQuantity_Box"]?.asString(),
            shippingQuantityBox = node["ShippingQuantity_Box"]?.asString(),
            defaultReason = node["DefaultReason"]?.asString(),
        )
}

/**
 * SAP `OrderRequestDetail` 응답 `result[]` 라인 1건 (Spec #595).
 *
 * 레거시 `IF_REST_MOBILE_OrderRequestDetail.cls:108-149` 추출 18개 필드 전수.
 * `EmployeeCode` 는 SAP 응답에 등장할 수 있으나 레거시가 추출하지 않으므로 본 매핑에서도 제외.
 *
 * 모든 필드는 SAP 호환을 위해 `String?` 으로 수신 (레거시 Apex 동등).
 */
data class SapOrderRequestDetailLine(
    val lineNumber: String?,
    val productCode: String?,
    val productName: String?,
    val lineItemStatus: String?,
    val totalQuantity: String?,
    val unit: String?,
    val sapOrderNumber: String?,
    val orderSalesAmount: String?,
    val deliveryRequestDate: String?,
    val orderDate: String?,
    val shippingDriverName: String?,
    val shippingVehicle: String?,
    val shippingDriverPhone: String?,
    val shippingScheduleTime: String?,
    val shippingCompleteTime: String?,
    val totalQuantityBox: String?,
    val shippingQuantityBox: String?,
    val defaultReason: String?,
)
