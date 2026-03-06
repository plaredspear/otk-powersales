package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapDailySalesRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.DailySalesHistory
import com.otoki.internal.sap.repository.DailySalesHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapDailySalesService(
    private val dailySalesHistoryRepository: DailySalesHistoryRepository
) : SapSyncService<SapDailySalesRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CHUNK_SIZE = 10_000
    }

    override fun sync(items: List<SapDailySalesRequest.ReqItem>): SapSyncResult {
        var totalSuccess = 0
        val allErrors = mutableListOf<SapSyncError>()

        items.chunked(CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
            try {
                val result = syncChunk(chunk, chunkIndex * CHUNK_SIZE)
                totalSuccess += result.successCount
                allErrors.addAll(result.errors)
            } catch (e: Exception) {
                log.error("일별 매출 청크 처리 실패: chunkIndex={}, error={}", chunkIndex, e.message)
                chunk.forEachIndexed { index, item ->
                    allErrors.add(
                        SapSyncError(
                            index = chunkIndex * CHUNK_SIZE + index,
                            field = "chunk",
                            value = item.sapAccountCode,
                            error = "Chunk failed: ${e.message}"
                        )
                    )
                }
            }
        }

        return SapSyncResult(
            successCount = totalSuccess,
            failCount = allErrors.size,
            errors = allErrors
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun syncChunk(chunk: List<SapDailySalesRequest.ReqItem>, baseIndex: Int): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        chunk.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("일별 매출 동기화 실패: index={}, sapAccountCode={}, error={}",
                    baseIndex + index, item.sapAccountCode, e.message)
                errors.add(
                    SapSyncError(
                        index = baseIndex + index,
                        field = "sap_account_code",
                        value = item.sapAccountCode,
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

    private fun syncItem(item: SapDailySalesRequest.ReqItem) {
        val sapAccountCode = item.sapAccountCode
            ?: throw IllegalArgumentException("sap_account_code is required")
        val salesDate = item.salesDate
            ?: throw IllegalArgumentException("sales_date is required")

        val externalKey = sapAccountCode + salesDate
        val existing = dailySalesHistoryRepository.findByExternalKey(externalKey)

        if (existing != null) {
            mapFields(existing, item)
            existing.updatedAt = LocalDateTime.now()
            dailySalesHistoryRepository.save(existing)
        } else {
            val entity = DailySalesHistory(
                sapAccountCode = sapAccountCode,
                salesDate = salesDate,
                externalKey = externalKey
            )
            mapFields(entity, item)
            dailySalesHistoryRepository.save(entity)
        }
    }

    private fun mapFields(entity: DailySalesHistory, item: SapDailySalesRequest.ReqItem) {
        entity.erpSalesAmount1 = parseDouble(item.erpSalesAmount1)
        entity.erpSalesAmount2 = parseDouble(item.erpSalesAmount2)
        entity.erpSalesAmount3 = parseDouble(item.erpSalesAmount3)
        entity.erpDistributionAmount1 = parseDouble(item.erpDistributionAmount1)
        entity.erpDistributionAmount2 = parseDouble(item.erpDistributionAmount2)
        entity.erpDistributionAmount3 = parseDouble(item.erpDistributionAmount3)
        entity.ledgerAmount = parseDouble(item.ledgerAmount)
    }

    private fun parseDouble(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.toDouble()
    }
}
