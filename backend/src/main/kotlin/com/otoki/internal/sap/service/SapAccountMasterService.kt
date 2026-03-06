package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapAccountMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.AccountCategoryMaster
import com.otoki.internal.sap.repository.AccountCategoryMasterRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapAccountMasterService(
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository
) : SapSyncService<SapAccountMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapAccountMasterRequest.ReqItem>): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("거래처분류 동기화 실패: index={}, accountCode={}, error={}",
                    index, item.accountCode, e.message)
                errors.add(
                    SapSyncError(
                        index = index,
                        field = "account_code",
                        value = item.accountCode,
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

    private fun syncItem(item: SapAccountMasterRequest.ReqItem) {
        val accountCode = item.accountCode
            ?: throw IllegalArgumentException("account_code is required")
        val accountName = item.name
            ?: throw IllegalArgumentException("name is required")

        val existing = accountCategoryMasterRepository.findByAccountCode(accountCode)

        if (existing != null) {
            existing.name = accountName
            existing.updatedAt = LocalDateTime.now()
            accountCategoryMasterRepository.save(existing)
        } else {
            val entity = AccountCategoryMaster(
                accountCode = accountCode,
                name = accountName
            )
            accountCategoryMasterRepository.save(entity)
        }
    }
}
