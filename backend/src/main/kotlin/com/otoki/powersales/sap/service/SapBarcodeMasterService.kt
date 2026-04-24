package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.entity.ProductBarcode
import com.otoki.powersales.sap.repository.ProductRepository
import com.otoki.powersales.sap.repository.ProductBarcodeRepository
import com.otoki.powersales.sap.dto.SapBarcodeMasterRequest
import com.otoki.powersales.sap.dto.SapSyncError
import com.otoki.powersales.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SapBarcodeMasterService(
    private val productBarcodeRepository: ProductBarcodeRepository,
    private val productRepository: ProductRepository
) : SapSyncService<SapBarcodeMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapBarcodeMasterRequest.ReqItem>): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("바코드 동기화 실패: index={}, productCode={}, error={}",
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

    private fun syncItem(item: SapBarcodeMasterRequest.ReqItem) {
        val productCode = item.productCode
            ?: throw IllegalArgumentException("product_code is required")
        val productUnit = item.productUnit
            ?: throw IllegalArgumentException("product_unit is required")
        val productSequence = item.productSequence
            ?: throw IllegalArgumentException("product_sequence is required")

        val customKey = "$productCode$productUnit$productSequence"

        val productId = productRepository.findByProductCode(productCode)?.id

        val existing = productBarcodeRepository.findByCustomKey(customKey)

        if (existing != null) {
            existing.productName = item.productName
            existing.barcode = item.productBarcode
            existing.unit = productUnit
            existing.sortOrder = productSequence
            existing.productId = productId
            productBarcodeRepository.save(existing)
        } else {
            val barcode = ProductBarcode(
                customKey = customKey,
                unit = productUnit,
                sortOrder = productSequence,
                productName = item.productName,
                barcode = item.productBarcode,
                productId = productId
            )
            productBarcodeRepository.save(barcode)
        }
    }
}
