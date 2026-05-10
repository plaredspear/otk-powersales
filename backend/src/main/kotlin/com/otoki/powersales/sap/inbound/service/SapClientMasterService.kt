package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.account.service.AccountUpsertService
import com.otoki.powersales.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.account.service.dto.AccountUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.sap.inbound.dto.account.ClientMasterRequestItem
import com.otoki.powersales.sap.inbound.dto.account.FailureItem
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 거래처 마스터 인바운드 어댑터. (Spec #558 / 어댑터-도메인 분리: #634)
 *
 * 책임:
 * - SAP 페이로드 [ClientMasterRequestItem] → 도메인 커맨드 [AccountUpsertCommand] 매핑
 * - 도메인 서비스 [AccountUpsertService.upsert] 호출
 * - 도메인 결과 [AccountUpsertResult] → SAP 응답 [AccountMasterDetail] 매핑
 * - [SapInboundAuditService] 감사 기록
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 * UPSERT 키 / Employee 매칭 / Organization 폴백 / 부분 실패 시멘틱은 [AccountUpsertService] KDoc 참조.
 */
@Service
class SapClientMasterService(
    private val accountUpsertService: AccountUpsertService,
    private val auditService: SapInboundAuditService
) {

    fun upsert(items: List<ClientMasterRequestItem>): AccountMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = try {
            accountUpsertService.upsert(commands)
        } catch (ex: RuntimeException) {
            recordAccepted(items.size, success = 0, failure = commands.size)
            throw ex
        }

        recordAccepted(items.size, success = result.successCount, failure = result.failureCount)
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

    private fun recordAccepted(received: Int, success: Int, failure: Int) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = "success=$success failure=$failure"
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }
}
