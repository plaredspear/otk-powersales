package com.otoki.internal.sap.service

import com.otoki.internal.entity.ProductBarcode
import com.otoki.internal.product.repository.ProductRepository
import com.otoki.internal.repository.ProductBarcodeRepository
import com.otoki.internal.sap.dto.SapBarcodeMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
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

        val productSfid = productRepository.findByProductCode(productCode)?.sfid

        val existing = productBarcodeRepository.findByCustomKey(customKey)

        if (existing != null) {
            existing.productName = item.productName
            existing.productBarcode = item.productBarcode
            existing.productUnit = productUnit
            existing.productSequence = productSequence
            existing.product = productSfid
            productBarcodeRepository.save(existing)
        } else {
            val barcode = ProductBarcode(
                customKey = customKey,
                productUnit = productUnit,
                productSequence = productSequence,
                productName = item.productName,
                productBarcode = item.productBarcode,
                product = productSfid
            )
            productBarcodeRepository.save(barcode)
        }
    }
}
