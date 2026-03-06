package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.MonthlySalesHistory
import com.otoki.internal.sap.repository.MonthlySalesHistoryRepository
import com.otoki.internal.sap.dto.SapMonthlySalesRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SapMonthlySalesService(
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository
) : SapSyncService<SapMonthlySalesRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CHUNK_SIZE = 10_000
    }

    override fun sync(items: List<SapMonthlySalesRequest.ReqItem>): SapSyncResult {
        var totalSuccess = 0
        val allErrors = mutableListOf<SapSyncError>()

        items.chunked(CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
            try {
                val result = syncChunk(chunk, chunkIndex * CHUNK_SIZE)
                totalSuccess += result.successCount
                allErrors.addAll(result.errors)
            } catch (e: Exception) {
                log.error("월별 매출 청크 처리 실패: chunkIndex={}, error={}", chunkIndex, e.message)
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
    fun syncChunk(chunk: List<SapMonthlySalesRequest.ReqItem>, baseIndex: Int): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        chunk.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("월별 매출 동기화 실패: index={}, sapAccountCode={}, error={}",
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

    private fun syncItem(item: SapMonthlySalesRequest.ReqItem) {
        val sapAccountCode = item.sapAccountCode
            ?: throw IllegalArgumentException("sap_account_code is required")
        val salesYearMonth = item.salesYearMonth
            ?: throw IllegalArgumentException("sales_year_month is required")

        val externalKey = sapAccountCode + salesYearMonth
        val existing = monthlySalesHistoryRepository.findByExternalkeyC(externalKey)

        if (existing != null) {
            mapFields(existing, item, sapAccountCode, salesYearMonth)
            monthlySalesHistoryRepository.save(existing)
        } else {
            val entity = MonthlySalesHistory(
                accountExternalKey = sapAccountCode,
                salesYear = salesYearMonth.substring(0, 4),
                salesMonth = salesYearMonth.substring(4, 6),
                externalkeyC = externalKey
            )
            mapFields(entity, item, sapAccountCode, salesYearMonth)
            monthlySalesHistoryRepository.save(entity)
        }
    }

    private fun mapFields(
        entity: MonthlySalesHistory,
        item: SapMonthlySalesRequest.ReqItem,
        sapAccountCode: String,
        salesYearMonth: String
    ) {
        entity.accountExternalKey = sapAccountCode
        entity.salesYear = salesYearMonth.substring(0, 4)
        entity.salesMonth = salesYearMonth.substring(4, 6)
        entity.abcClosingAmount1 = parseDouble(item.abcClosingAmount1)
        entity.abcClosingAmount2 = parseDouble(item.abcClosingAmount2)
        entity.abcClosingAmount3 = parseDouble(item.abcClosingAmount3)
        entity.shipClosingAmount = parseDouble(item.shipClosingAmount)
        entity.rlsalesC = parseDouble(item.rlsales)
    }

    private fun parseDouble(value: String?): Double {
        if (value.isNullOrBlank()) return 0.0
        return value.toDouble()
    }
}
