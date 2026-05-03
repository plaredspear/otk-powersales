package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterRequestItem
import com.otoki.powersales.product.repository.ProductRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * SAP 제품 마스터 인바운드 UPSERT 서비스. (Spec #559, Spec #575)
 *
 * - UPSERT 키: [Product.productCode]
 * - 부분 실패 허용 (행 단위 검증 후 saveAll 일괄)
 * - StoreCondition 페이로드는 엔티티 [Product.storageCondition] 에 매핑 (D1)
 * - StandardPrice 는 [Product.standardPrice] 에 매핑 (var, mutable)
 * - Spec #575: ProductBarcode → [Product.productBarcode], Pallet → [Product.pallet]
 *   (Pallet 은 null/blank/"0" → 0.0, 비숫자 → 행 단위 부분 실패)
 */
@Service
class SapProductMasterService(
    private val productRepository: ProductRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<ProductMasterRequestItem>): ProductMasterDetail {
        val productCodes = items.mapNotNull { it.productCode?.takeIf { code -> code.isNotBlank() } }
        val cache: MutableMap<String, Product> = if (productCodes.isEmpty()) {
            mutableMapOf()
        } else {
            productRepository.findByProductCodeIn(productCodes.distinct())
                .mapNotNull { p -> p.productCode?.let { it to p } }
                .toMap()
                .toMutableMap()
        }

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<Product>()

        items.forEach { item ->
            val productCode = item.productCode?.takeIf { it.isNotBlank() }
            if (productCode == null) {
                failures += FailureItem(null, "ProductCode 필수")
                return@forEach
            }
            val productName = item.productName?.takeIf { it.isNotBlank() }
            if (productName == null) {
                failures += FailureItem(productCode, "ProductName 필수")
                return@forEach
            }

            val standardPrice = parseDouble(item.standardPrice, allowBlankAsNull = true)
            if (standardPrice.isFailure) {
                failures += FailureItem(productCode, "StandardPrice 변환 실패: ${item.standardPrice}")
                return@forEach
            }
            val boxQty = parseDouble(item.boxReceivingQuantity, allowBlankAsNull = true)
            if (boxQty.isFailure) {
                failures += FailureItem(productCode, "BoxReceivingQuantity 변환 실패: ${item.boxReceivingQuantity}")
                return@forEach
            }
            val superTax = parseDouble(item.superTax, allowBlankAsZero = true)
            if (superTax.isFailure) {
                failures += FailureItem(productCode, "SuperTax 변환 실패: ${item.superTax}")
                return@forEach
            }
            val launchDate = parseLaunchDate(item.launchDate)
            if (launchDate.isFailure) {
                failures += FailureItem(productCode, "LaunchDate 형식 오류: ${item.launchDate}")
                return@forEach
            }
            // Spec #575: Pallet 변환. null/blank/"0" → 0.0. 비숫자 → 행 단위 부분 실패.
            val pallet = parseDouble(item.pallet, allowBlankAsZero = true)
            if (pallet.isFailure) {
                failures += FailureItem(productCode, "Pallet 형식 오류: ${item.pallet}")
                return@forEach
            }

            val entity = cache[productCode]
                ?.also { applyToEntity(it, item, productName, standardPrice.value, boxQty.value, superTax.value, launchDate.value, pallet.value) }
                ?: createProduct(productCode, productName, item, standardPrice.value, boxQty.value, superTax.value, launchDate.value, pallet.value)
                    .also { cache[productCode] = it }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            productRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)
        return ProductMasterDetail(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun createProduct(
        productCode: String,
        productName: String,
        item: ProductMasterRequestItem,
        standardPrice: Double?,
        boxQty: Double?,
        superTax: Double?,
        launchDate: LocalDate?,
        pallet: Double?
    ): Product {
        val product = Product(productCode = productCode, name = productName)
        applyMutableFields(product, item, standardPrice, boxQty, superTax, launchDate, pallet)
        return product
    }

    private fun applyToEntity(
        product: Product,
        item: ProductMasterRequestItem,
        productName: String,
        standardPrice: Double?,
        boxQty: Double?,
        superTax: Double?,
        launchDate: LocalDate?,
        pallet: Double?
    ) {
        product.name = productName
        applyMutableFields(product, item, standardPrice, boxQty, superTax, launchDate, pallet)
    }

    private fun applyMutableFields(
        product: Product,
        item: ProductMasterRequestItem,
        standardPrice: Double?,
        boxQty: Double?,
        superTax: Double?,
        launchDate: LocalDate?,
        pallet: Double?
    ) {
        product.productStatus = item.productStatus
        product.productType = item.productType
        product.category1 = item.category1
        product.category2 = item.category2
        product.category3 = item.category3
        product.categoryCode1 = item.categoryCode1
        product.categoryCode2 = item.categoryCode2
        product.categoryCode3 = item.categoryCode3
        product.unit = item.unit
        product.shelfLife = item.shelfLife
        product.shelfLifeUnit = item.shelfLifeUnit
        product.tasteGift = item.tasteGift
        product.logisticsBarcode = item.logisticsBarCode
        product.storageCondition = item.storeCondition
        product.standardPrice = standardPrice
        product.boxReceivingQuantity = boxQty
        product.superTax = superTax ?: 0.0
        product.launchDate = launchDate
        // Spec #575
        product.productBarcode = item.productBarcode
        product.pallet = pallet
    }

    private data class ParseResult<T>(val value: T?, val isFailure: Boolean)

    private fun parseDouble(
        value: String?,
        allowBlankAsNull: Boolean = false,
        allowBlankAsZero: Boolean = false
    ): ParseResult<Double> {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty()) {
            return when {
                allowBlankAsZero -> ParseResult(0.0, false)
                allowBlankAsNull -> ParseResult(null, false)
                else -> ParseResult(null, true)
            }
        }
        return try {
            ParseResult(trimmed.toDouble(), false)
        } catch (_: NumberFormatException) {
            ParseResult(null, true)
        }
    }

    private fun parseLaunchDate(value: String?): ParseResult<LocalDate> {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty() || trimmed == "00000000") {
            return ParseResult(null, false)
        }
        return try {
            ParseResult(LocalDate.parse(trimmed, DATE_FORMAT), false)
        } catch (_: DateTimeParseException) {
            ParseResult(null, true)
        }
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

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
