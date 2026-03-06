package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapAttendInfoRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.AttendInfo
import com.otoki.internal.sap.repository.AttendInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SapAttendInfoService(
    private val attendInfoRepository: AttendInfoRepository
) : SapSyncService<SapAttendInfoRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CHUNK_SIZE = 5_000
    }

    override fun sync(items: List<SapAttendInfoRequest.ReqItem>): SapSyncResult {
        var totalSuccess = 0
        val allErrors = mutableListOf<SapSyncError>()

        items.chunked(CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
            try {
                val result = syncChunk(chunk, chunkIndex * CHUNK_SIZE)
                totalSuccess += result.successCount
                allErrors.addAll(result.errors)
            } catch (e: Exception) {
                log.error("출퇴근 청크 처리 실패: chunkIndex={}, error={}", chunkIndex, e.message)
                chunk.forEachIndexed { index, item ->
                    allErrors.add(
                        SapSyncError(
                            index = chunkIndex * CHUNK_SIZE + index,
                            field = "chunk",
                            value = item.employeeCode,
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
    fun syncChunk(chunk: List<SapAttendInfoRequest.ReqItem>, baseIndex: Int): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        chunk.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("출퇴근 동기화 실패: index={}, employeeCode={}, error={}",
                    baseIndex + index, item.employeeCode, e.message)
                errors.add(
                    SapSyncError(
                        index = baseIndex + index,
                        field = "employee_code",
                        value = item.employeeCode,
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

    private fun syncItem(item: SapAttendInfoRequest.ReqItem) {
        val employeeCode = item.employeeCode
            ?: throw IllegalArgumentException("employee_code is required")
        val startDate = item.startDate
            ?: throw IllegalArgumentException("start_date is required")

        val attendInfo = AttendInfo(
            employeeCode = employeeCode,
            startDate = startDate,
            endDate = item.endDate,
            attendType = item.attendType,
            status = item.status
        )

        attendInfoRepository.save(attendInfo)
    }
}
