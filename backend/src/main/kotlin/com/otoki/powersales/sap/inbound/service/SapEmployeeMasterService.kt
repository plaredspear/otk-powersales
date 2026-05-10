package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.employee.service.EmployeeUpsertService
import com.otoki.powersales.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.employee.service.dto.EmployeeUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterRequestItem
import com.otoki.powersales.sap.inbound.dto.employee.FailureItem
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 직원 마스터 인바운드 어댑터. (Spec #557 + #579 / 어댑터-도메인 분리: #635 P2-B)
 *
 * 책임:
 * - SAP 페이로드 [EmployeeMasterRequestItem] → 도메인 커맨드 [EmployeeUpsertCommand] 매핑
 * - 도메인 서비스 [EmployeeUpsertService.upsert] 호출
 * - 도메인 결과 [EmployeeUpsertResult] → SAP 응답 [EmployeeMasterDetail] 매핑
 * - [SapInboundAuditService] 감사 기록 — 본 어댑터는 audit 을 2종 기록한다:
 *   1. `REQUEST_ACCEPTED` (success/failure 집계)
 *   2. `MANUAL_ORIGIN_PROTECTED` (#579 보호 직원 코드 목록, 보호 발생 시에만)
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 */
@Service
class SapEmployeeMasterService(
    private val employeeUpsertService: EmployeeUpsertService,
    private val auditService: SapInboundAuditService
) {

    fun upsert(items: List<EmployeeMasterRequestItem>): EmployeeMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = try {
            employeeUpsertService.upsert(commands)
        } catch (ex: RuntimeException) {
            recordAccepted(items.size, success = 0, failure = commands.size)
            throw ex
        }

        recordAccepted(items.size, success = result.successCount, failure = result.failureCount)

        if (result.protectedManualCodes.isNotEmpty()) {
            recordManualOriginProtected(items.size, result.protectedManualCodes)
        }

        return EmployeeMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun EmployeeMasterRequestItem.toCommand(): EmployeeUpsertCommand = EmployeeUpsertCommand(
        employeeCode = employeeCode,
        employeeName = employeeName,
        gender = gender,
        homePhone = homePhone,
        workPhone = workPhone,
        workEmail = workEmail,
        email = email,
        startDate = startDate,
        endDate = endDate,
        status = status,
        birthdate = birthdate,
        orgCode = orgCode,
        lockingFlag = lockingFlag
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

    private fun recordManualOriginProtected(received: Int, protectedCodes: List<String>) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        val reason = protectedCodes.joinToString(",").take(REASON_MAX_LENGTH)
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.MANUAL_ORIGIN_PROTECTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = reason
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }

    companion object {
        private const val REASON_MAX_LENGTH = 1000
    }
}
