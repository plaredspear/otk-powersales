package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.entity.ProductBarcode
import com.otoki.powersales.sap.inbound.dto.product.BarcodeMasterRequestItem
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.product.repository.ProductBarcodeRepository
import com.otoki.powersales.product.repository.ProductRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 제품 바코드 마스터 인바운드 UPSERT 서비스. (Spec #559)
 *
 * - UPSERT 키: [ProductBarcode.customKey] = ProductCode + ProductUnit + ProductSequence
 * - ProductCode 매칭 실패 시 해당 행 failures (FK 무결성)
 * - ProductUnit 은 레거시 호환을 위해 [ProductBarcode.name] 컬럼에도 동일 값 저장 (D2)
 * - 부분 실패 허용 (행 단위 검증 후 saveAll 일괄)
 */
@Service
class SapBarcodeMasterService(
    private val productBarcodeRepository: ProductBarcodeRepository,
    private val productRepository: ProductRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<BarcodeMasterRequestItem>): ProductMasterDetail {
        val productCodes = items.mapNotNull { it.productCode?.takeIf { code -> code.isNotBlank() } }
        val productCache: Map<String, Product> = if (productCodes.isEmpty()) {
            emptyMap()
        } else {
            productRepository.findByProductCodeIn(productCodes.distinct())
                .mapNotNull { p -> p.productCode?.let { it to p } }
                .toMap()
        }

        val customKeys = items.mapNotNull { customKey(it) }
        val barcodeCache: MutableMap<String, ProductBarcode> = if (customKeys.isEmpty()) {
            mutableMapOf()
        } else {
            customKeys.distinct()
                .mapNotNull { key -> productBarcodeRepository.findByCustomKey(key)?.let { key to it } }
                .toMap()
                .toMutableMap()
        }

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<ProductBarcode>()

        items.forEach { item ->
            val productCode = item.productCode?.takeIf { it.isNotBlank() }
            val productUnit = item.productUnit?.takeIf { it.isNotBlank() }
            val productSequence = item.productSequence?.takeIf { it.isNotBlank() }
            val barcode = item.productBarcode?.takeIf { it.isNotBlank() }

            if (productCode == null) {
                failures += FailureItem(null, "ProductCode 필수")
                return@forEach
            }
            if (productUnit == null) {
                failures += FailureItem(null, "ProductUnit 필수")
                return@forEach
            }
            if (productSequence == null) {
                failures += FailureItem(null, "ProductSequence 필수")
                return@forEach
            }
            if (barcode == null) {
                failures += FailureItem(productCode + productUnit + productSequence, "ProductBarcode 필수")
                return@forEach
            }

            val key = productCode + productUnit + productSequence
            val matchedProduct = productCache[productCode]
            if (matchedProduct == null) {
                failures += FailureItem(key, "product_code not found: $productCode")
                return@forEach
            }

            val entity = barcodeCache[key]?.also {
                applyToEntity(it, item, key, matchedProduct, productUnit, barcode)
            } ?: createBarcode(key, item, matchedProduct, productUnit, barcode)
                .also { barcodeCache[key] = it }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            productBarcodeRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)
        return ProductMasterDetail(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun customKey(item: BarcodeMasterRequestItem): String? {
        val pc = item.productCode?.takeIf { it.isNotBlank() } ?: return null
        val pu = item.productUnit?.takeIf { it.isNotBlank() } ?: return null
        val ps = item.productSequence?.takeIf { it.isNotBlank() } ?: return null
        return pc + pu + ps
    }

    private fun createBarcode(
        key: String,
        item: BarcodeMasterRequestItem,
        matchedProduct: Product,
        productUnit: String,
        barcode: String
    ): ProductBarcode {
        val entity = ProductBarcode(
            customKey = key,
            name = productUnit,
            unit = productUnit,
            barcode = barcode,
            productSfid = matchedProduct.sfid,
            productId = matchedProduct.id
        )
        entity.productName = item.productName
        entity.sortOrder = item.productSequence
        return entity
    }

    private fun applyToEntity(
        entity: ProductBarcode,
        item: BarcodeMasterRequestItem,
        key: String,
        matchedProduct: Product,
        productUnit: String,
        barcode: String
    ) {
        entity.customKey = key
        entity.name = productUnit
        entity.unit = productUnit
        entity.barcode = barcode
        entity.sortOrder = item.productSequence
        entity.productName = item.productName
        entity.productSfid = matchedProduct.sfid
        entity.productId = matchedProduct.id
    }

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
