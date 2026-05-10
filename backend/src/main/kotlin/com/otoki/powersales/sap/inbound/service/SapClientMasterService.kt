package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.account.service.AccountUpsertService
import com.otoki.powersales.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.sap.inbound.dto.account.ClientMasterRequestItem
import com.otoki.powersales.sap.inbound.dto.account.FailureItem
import org.springframework.stereotype.Service

/**
 * SAP 거래처 마스터 인바운드 어댑터. (Spec #558 / 어댑터-도메인 분리: #634 / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [ClientMasterRequestItem] → 도메인 커맨드 [AccountUpsertCommand] 매핑
 * - 도메인 서비스 [AccountUpsertService.upsert] 호출
 * - 도메인 결과 → SAP 응답 [AccountMasterDetail] 매핑
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 * UPSERT 키 / Employee 매칭 / Organization 폴백 / 부분 실패 시멘틱은 [AccountUpsertService] KDoc 참조.
 */
@Service
class SapClientMasterService(
    private val accountUpsertService: AccountUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<ClientMasterRequestItem>): AccountMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = accountUpsertService.upsert(commands)
        return AccountMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun ClientMasterRequestItem.toCommand(): AccountUpsertCommand = AccountUpsertCommand(
        externalKey = sapAccountCode,
        name = name,
        accountType = accountType,
        accountStatusCode = accountStatusCode,
        accountStatusName = accountStatusName,
        accountGroup = accountGroup,
        phone = phone,
        mobilePhone = mobilePhone,
        email = email,
        businessType = businessType,
        businessCategory = businessCategory,
        employeeCode = employeeCode,
        businessLicenseNumber = businessLicenseNumber,
        representative = representative,
        zipcode = zipcode,
        address1 = address1,
        address2 = address2,
        divisionCode = divisionCode,
        divisionName = divisionName,
        salesDeptCode = salesDeptCode,
        salesDeptName = salesDeptName,
        branchCode = branchCode,
        branchName = branchName,
        closingTime1 = closingTime1,
        closingTime2 = closingTime2,
        closingTime3 = closingTime3,
        abcType = abcType,
        abcTypeCode = abcTypeCode,
        distribution = distribution,
        consignmentAcc = consignmentAcc,
        werk1 = werk1,
        werk2 = werk2,
        werk3 = werk3,
        werk1Tx = werk1Tx,
        werk2Tx = werk2Tx,
        werk3Tx = werk3Tx
    )
}
