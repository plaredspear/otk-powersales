package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentDetail
import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.schedule.service.AppointmentInsertService
import com.otoki.powersales.schedule.service.dto.AppointmentInsertCommand
import com.otoki.powersales.schedule.service.dto.AppointmentInsertResult
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 인사발령 인바운드 어댑터. (Spec #562 / 어댑터-도메인 분리: #635 P2-B)
 *
 * 책임:
 * - SAP 페이로드 [AppointmentRequestItem] → 도메인 커맨드 [AppointmentInsertCommand] 매핑
 * - 도메인 서비스 [AppointmentInsertService.insert] 호출 (INSERT only, 멱등성 미보장)
 * - 도메인 결과 [AppointmentInsertResult] → SAP 응답 [AppointmentDetail] 매핑
 * - 후처리 트리거 [AppointmentUserProfileUpdater.updateUserProfiles] 호출 (적재 commit 후, 실패 시 적재는 유지 + 로그)
 * - [SapInboundAuditService] 감사 기록
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 */
@Service
class SapAppointmentService(
    private val appointmentInsertService: AppointmentInsertService,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater,
    private val auditService: SapInboundAuditService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun insert(items: List<AppointmentRequestItem>): AppointmentDetail {
        val commands = items.map { it.toCommand() }
        val result = try {
            appointmentInsertService.insert(commands)
        } catch (ex: RuntimeException) {
            recordAccepted(items.size, success = 0, failure = commands.size)
            throw ex
        }

        if (result.savedAppointments.isNotEmpty()) {
            try {
                appointmentUserProfileUpdater.updateUserProfiles(result.savedAppointments)
            } catch (ex: Exception) {
                log.warn("AppointmentUserProfileUpdater 실패 (적재는 유지): {}", ex.message, ex)
            }
        }

        recordAccepted(items.size, success = result.successCount, failure = result.failureCount)

        return AppointmentDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun AppointmentRequestItem.toCommand(): AppointmentInsertCommand = AppointmentInsertCommand(
        employeeCode = employeeCode,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jikchak = jikchak,
        jikwee = jikwee,
        jikgub = jikgub,
        workType = workType,
        manageType = manageType,
        jobCode = jobCode,
        workArea = workArea,
        jikjong = jikjong,
        appointDate = appointDate,
        jobName = jobName,
        ordDetailCode = ordDetailCode,
        ordDetailNode = ordDetailNode
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
