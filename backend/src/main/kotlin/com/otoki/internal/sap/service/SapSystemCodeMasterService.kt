package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapSystemCodeMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.SystemCodeMaster
import com.otoki.internal.sap.repository.SystemCodeMasterRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapSystemCodeMasterService(
    private val systemCodeMasterRepository: SystemCodeMasterRepository
) : SapSyncService<SapSystemCodeMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapSystemCodeMasterRequest.ReqItem>): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("시스템코드 동기화 실패: index={}, groupCode={}, detailCode={}, error={}",
                    index, item.groupCode, item.detailCode, e.message)
                errors.add(
                    SapSyncError(
                        index = index,
                        field = "external_key",
                        value = "${item.companyCode};${item.groupCode};${item.detailCode}",
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

    private fun syncItem(item: SapSystemCodeMasterRequest.ReqItem) {
        val companyCode = item.companyCode
            ?: throw IllegalArgumentException("company_code is required")
        val groupCode = item.groupCode
            ?: throw IllegalArgumentException("group_code is required")
        val detailCode = item.detailCode
            ?: throw IllegalArgumentException("detail_code is required")

        val externalKey = "$companyCode;$groupCode;$detailCode"
        val now = LocalDateTime.now()

        val existing = systemCodeMasterRepository.findByExternalKey(externalKey)

        if (existing != null) {
            existing.groupCodeName = item.groupCodeName
            existing.detailCodeName = item.detailCodeName
            existing.seq = item.seq
            existing.updatedAt = now
            systemCodeMasterRepository.save(existing)
        } else {
            val entity = SystemCodeMaster(
                companyCode = companyCode,
                groupCode = groupCode,
                detailCode = detailCode,
                groupCodeName = item.groupCodeName,
                detailCodeName = item.detailCodeName,
                seq = item.seq,
                externalKey = externalKey,
                createdAt = now
            )
            systemCodeMasterRepository.save(entity)
        }
    }
}
