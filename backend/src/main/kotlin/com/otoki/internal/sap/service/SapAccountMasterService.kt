package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.dto.SapAccountMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SapAccountMasterService(
    private val accountRepository: AccountRepository
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
                log.warn("거래처 동기화 실패: index={}, sapAccountCode={}, error={}",
                    index, item.sapAccountCode, e.message)
                errors.add(
                    SapSyncError(
                        index = index,
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

    private fun syncItem(item: SapAccountMasterRequest.ReqItem) {
        val sapAccountCode = item.sapAccountCode
            ?: throw IllegalArgumentException("sap_account_code is required")
        val accountName = item.name
            ?: throw IllegalArgumentException("name is required")

        val existing = accountRepository.findByExternalKey(sapAccountCode)

        if (existing != null) {
            mapFields(existing, item, accountName)
            accountRepository.save(existing)
        } else {
            val account = Account(
                externalKey = sapAccountCode
            )
            mapFields(account, item, accountName)
            accountRepository.save(account)
        }
    }

    private fun mapFields(
        account: Account,
        item: SapAccountMasterRequest.ReqItem,
        name: String
    ) {
        account.name = name
        account.accountType = item.accountType
        account.accountStatusName = item.accountStatusName
        account.accountGroup = item.accountGroup
        account.phone = item.phone
        account.mobilePhone = item.mobilePhone
        account.employeeCode = item.employeeCode
        account.representative = item.representative
        account.zipCode = item.zipcode
        account.address1 = item.address1
        account.address2 = item.address2
        account.branchCode = item.branchCode
        account.branchName = item.branchName
        account.closingTime1 = item.closingTime1
        account.closingTime2 = item.closingTime2
        account.closingTime3 = item.closingTime3
        account.abcType = item.abcType
        account.abcTypeCode = item.abcTypeCode
        account.distribution = item.distribution
        account.werk1Tx = item.werk1Tx
        account.werk2Tx = item.werk2Tx
        account.werk3Tx = item.werk3Tx
    }
}
