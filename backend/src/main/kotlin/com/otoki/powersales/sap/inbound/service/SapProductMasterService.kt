package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.product.service.ProductUpsertService
import com.otoki.powersales.product.service.dto.ProductUpsertCommand
import com.otoki.powersales.product.service.dto.ProductUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterRequestItem
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 제품 마스터 인바운드 어댑터. (Spec #559, #575 / 어댑터-도메인 분리: #635 P1-B)
 *
 * 책임:
 * - SAP 페이로드 [ProductMasterRequestItem] → 도메인 커맨드 [ProductUpsertCommand] 매핑
 * - 도메인 서비스 [ProductUpsertService.upsert] 호출
 * - 도메인 결과 [ProductUpsertResult] → SAP 응답 [ProductMasterDetail] 매핑
 * - [SapInboundAuditService] 감사 기록
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다.
 * UPSERT 키 / 변환 정책 / 부분 실패 시멘틱은 [ProductUpsertService] KDoc 참조.
 */
@Service
class SapProductMasterService(
    private val productUpsertService: ProductUpsertService,
    private val auditService: SapInboundAuditService
) {

    fun upsert(items: List<ProductMasterRequestItem>): ProductMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = try {
            productUpsertService.upsert(commands)
        } catch (ex: RuntimeException) {
            recordAccepted(items.size, success = 0, failure = commands.size)
            throw ex
        }

        recordAccepted(items.size, success = result.successCount, failure = result.failureCount)
        return ProductMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun ProductMasterRequestItem.toCommand(): ProductUpsertCommand = ProductUpsertCommand(
        productCode = productCode,
        productName = productName,
        productBarcode = productBarcode,
        logisticsBarCode = logisticsBarCode,
        categoryCode1 = categoryCode1,
        category1 = category1,
        categoryCode2 = categoryCode2,
        category2 = category2,
        categoryCode3 = categoryCode3,
        category3 = category3,
        productStatus = productStatus,
        standardPrice = standardPrice,
        unit = unit,
        boxReceivingQuantity = boxReceivingQuantity,
        shelfLife = shelfLife,
        shelfLifeUnit = shelfLifeUnit,
        launchDate = launchDate,
        storeCondition = storeCondition,
        productType = productType,
        superTax = superTax,
        tasteGift = tasteGift,
        pallet = pallet
    )

    private fun recordAccepted(received: Int, success: Int, failure: Int) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = "success=$success failure=$failure"
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }
}
