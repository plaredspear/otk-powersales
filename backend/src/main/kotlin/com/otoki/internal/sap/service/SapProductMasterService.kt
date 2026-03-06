package com.otoki.internal.sap.service

import com.otoki.internal.product.entity.Product
import com.otoki.internal.product.repository.ProductRepository
import com.otoki.internal.sap.dto.SapProductMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class SapProductMasterService(
    private val productRepository: ProductRepository
) : SapSyncService<SapProductMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    @Transactional
    override fun sync(items: List<SapProductMasterRequest.ReqItem>): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("제품 동기화 실패: index={}, productCode={}, error={}",
                    index, item.productCode, e.message)
                errors.add(
                    SapSyncError(
                        index = index,
                        field = "product_code",
                        value = item.productCode,
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        return SapSyncResult(
            successCount = successCount,
            failCount = errors.size,
            errors = errors
        )
    }

    private fun syncItem(item: SapProductMasterRequest.ReqItem) {
        val productCode = item.productCode
            ?: throw IllegalArgumentException("product_code is required")

        val existing = productRepository.findByProductCode(productCode)

        if (existing != null) {
            mapFields(existing, item)
            productRepository.save(existing)
        } else {
            val product = Product(productCode = productCode)
            mapFields(product, item)
            productRepository.save(product)
        }
    }

    private fun mapFields(product: Product, item: SapProductMasterRequest.ReqItem) {
        product.name = item.productName
        product.logisticsBarcode = item.logisticsBarcode
        product.categoryCode1 = item.categoryCode1
        product.category1 = item.category1
        product.categoryCode2 = item.categoryCode2
        product.category2 = item.category2
        product.categoryCode3 = item.categoryCode3
        product.category3 = item.category3
        product.productStatus = item.productStatus
        product.standardPrice = parseDouble(item.standardPrice)
        product.unit = item.unit
        product.boxReceivingQuantity = parseDouble(item.boxReceivingQuantity)
        product.shelfLife = item.shelfLife
        product.shelfLifeUnit = item.shelfLifeUnit
        product.launchDate = parseDate(item.launchDate)
        product.storageCondition = item.storeCondition
        product.productType = item.productType
        product.superTax = parseDouble(item.superTax)
        product.tasteGift = item.tasteGift
    }

    private fun parseDouble(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.toDouble()
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return LocalDate.parse(value, dateFormatter)
    }
}
