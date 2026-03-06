package com.otoki.internal.sap.service

import com.otoki.internal.entity.Account
import com.otoki.internal.repository.AccountRepository
import com.otoki.internal.sap.dto.SapClientMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SapClientMasterService(
    private val accountRepository: AccountRepository,
    private val orgRepository: OrgRepository
) : SapSyncService<SapClientMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapClientMasterRequest.ReqItem>): SapSyncResult {
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

    private fun syncItem(item: SapClientMasterRequest.ReqItem) {
        val sapAccountCode = item.sapAccountCode
            ?: throw IllegalArgumentException("sap_account_code is required")
        val accountName = item.name
            ?: throw IllegalArgumentException("name is required")

        val existing = accountRepository.findByExternalKey(sapAccountCode)
        val now = LocalDateTime.now()

        val org = resolveOrg(item.branchCode, item.salesDeptCode)

        if (existing != null) {
            mapFields(existing, item, accountName, org)
            existing.updatedAt = now
            accountRepository.save(existing)
        } else {
            val account = Account(
                externalKey = sapAccountCode,
                createdAt = now,
                updatedAt = now
            )
            mapFields(account, item, accountName, org)
            accountRepository.save(account)
        }
    }

    private fun mapFields(
        account: Account,
        item: SapClientMasterRequest.ReqItem,
        name: String,
        org: Org?
    ) {
        account.name = name
        account.accountType = item.accountType
        account.accountStatusCode = item.accountStatusCode
        account.accountStatusName = item.accountStatusName
        account.accountGroup = item.accountGroup
        account.phone = item.phone
        account.mobilePhone = item.mobilePhone
        account.email = item.email
        account.businessType = item.businessType
        account.businessCategory = item.businessCategory
        account.employeeCode = item.employeeCode
        account.businessLicenseNumber = item.businessLicenseNumber
        account.representative = item.representative
        account.zipCode = item.zipcode
        account.address1 = item.address1
        account.address2 = item.address2
        account.divisionCode = item.divisionCode
        account.divisionName = item.divisionName
        account.salesDeptCode = item.salesDeptCode
        account.salesDeptName = item.salesDeptName
        account.branchCode = item.branchCode
        account.branchName = item.branchName
        account.closingTime1 = item.closingTime1
        account.closingTime2 = item.closingTime2
        account.closingTime3 = item.closingTime3
        account.abcType = item.abcType
        account.abcTypeCode = item.abcTypeCode
        account.distribution = item.distribution
        account.consignmentAcc = item.consignmentAcc
        account.werk1 = item.werk1
        account.werk2 = item.werk2
        account.werk3 = item.werk3
        account.werk1Tx = item.werk1Tx
        account.werk2Tx = item.werk2Tx
        account.werk3Tx = item.werk3Tx

        account.orgCd3 = org?.orgCodeLevel3
        account.orgCd4 = org?.orgCodeLevel4
        account.orgCd5 = org?.orgCodeLevel5
    }

    internal fun resolveOrg(branchCode: String?, salesDeptCode: String?): Org? {
        if (branchCode.isNullOrBlank()) return null

        orgRepository.findFirstByCostCenterLevel5(branchCode)?.let { return it }
        orgRepository.findFirstByCostCenterLevel4(branchCode)?.let { return it }

        if (!salesDeptCode.isNullOrBlank()) {
            orgRepository.findFirstByCostCenterLevel4(salesDeptCode)?.let { return it }
        }

        return null
    }
}
