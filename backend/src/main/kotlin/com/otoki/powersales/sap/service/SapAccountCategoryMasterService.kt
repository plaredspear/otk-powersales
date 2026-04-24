package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.dto.SapAccountCategoryMasterRequest
import com.otoki.powersales.sap.dto.SapSyncError
import com.otoki.powersales.sap.dto.SapSyncResult
import com.otoki.powersales.sap.entity.AccountCategoryMaster
import com.otoki.powersales.sap.repository.AccountCategoryMasterRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class SapAccountCategoryMasterService(
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository
) : SapSyncService<SapAccountCategoryMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapAccountCategoryMasterRequest.ReqItem>): SapSyncResult {
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

    private fun syncItem(item: SapAccountCategoryMasterRequest.ReqItem) {
        val accountCode = item.accountCode
            ?: throw IllegalArgumentException("account_code is required")
        val accountName = item.name
            ?: throw IllegalArgumentException("name is required")

        val existing = accountCategoryMasterRepository.findByAccountCode(accountCode)

        if (existing != null) {
            existing.name = accountName
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
