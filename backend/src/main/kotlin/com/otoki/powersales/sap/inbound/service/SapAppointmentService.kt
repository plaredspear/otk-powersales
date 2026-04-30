package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentDetail
import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.schedule.repository.AppointmentRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sap.service.AppointmentUserProfileUpdater
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * SAP 인사발령 인바운드 INSERT 서비스. (Spec #562)
 *
 * 레거시 `IF_REST_SAP_Appointment` 와 동등한 INSERT only 모델 (옵션 C). idempotency 미보장
 * — 멱등성 강화는 후속 스펙 #567 에서 다룬다.
 *
 * - 모든 행의 EmployeeCode 집합으로 [Employee.empCode] 1회 조회 → `empCodeExist` 매핑
 * - 행마다 필수 필드 (EmployeeCode, JobCode, AppointDate) 검증 + AppointDate YYYYMMDD 형식 검증
 * - INSERT 후 [AppointmentUserProfileUpdater.updateUserProfiles] 자동 트리거 (D4 결정, 레거시 동작 유지)
 */
@Service
class SapAppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater,
    private val auditService: SapInboundAuditService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun insert(items: List<AppointmentRequestItem>): AppointmentDetail {
        val empCodes = items.mapNotNull { it.employeeCode?.takeIf { c -> c.isNotBlank() } }.distinct()
        val existingEmpCodes: Set<String> = if (empCodes.isEmpty()) {
            emptySet()
        } else {
            employeeRepository.findByEmployeeCodeIn(empCodes)
                .map { it.employeeCode }
                .toHashSet()
        }

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<Appointment>()

        items.forEach { item ->
            val employeeCode = item.employeeCode?.takeIf { it.isNotBlank() }
            val jobCode = item.jobCode?.takeIf { it.isNotBlank() }
            val appointDate = item.appointDate?.takeIf { it.isNotBlank() }

            if (employeeCode == null) {
                failures += FailureItem(null, "EmployeeCode 필수")
                return@forEach
            }
            if (jobCode == null) {
                failures += FailureItem(employeeCode, "JobCode 필수")
                return@forEach
            }
            if (appointDate == null) {
                failures += FailureItem(employeeCode, "AppointDate 필수")
                return@forEach
            }
            if (!isValidYyyymmdd(appointDate)) {
                failures += FailureItem(employeeCode + appointDate, "AppointDate YYYYMMDD 형식 오류: $appointDate")
                return@forEach
            }

            toSave += Appointment(
                employeeCode = employeeCode,
                empCodeExist = employeeCode in existingEmpCodes,
                afterOrgCode = item.afterOrgCode,
                afterOrgName = item.afterOrgName,
                jikchak = item.jikchak,
                jikwee = item.jikwee,
                jikgub = item.jikgub,
                workType = item.workType,
                manageType = item.manageType,
                jobCode = jobCode,
                workArea = item.workArea,
                jikjong = item.jikjong,
                appointDate = appointDate,
                jobName = item.jobName,
                ordDetailCode = item.ordDetailCode,
                ordDetailNode = item.ordDetailNode
            )
        }

        val saved = if (toSave.isNotEmpty()) {
            appointmentRepository.saveAll(toSave).toList()
        } else {
            emptyList()
        }

        if (saved.isNotEmpty()) {
            try {
                appointmentUserProfileUpdater.updateUserProfiles(saved)
            } catch (ex: Exception) {
                log.warn("AppointmentUserProfileUpdater 실패 (적재는 유지): {}", ex.message, ex)
            }
        }

        recordAccepted(items.size, saved.size, failures.size)

        return AppointmentDetail(
            successCount = saved.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun isValidYyyymmdd(value: String): Boolean = try {
        LocalDate.parse(value, DATE_FORMAT)
        true
    } catch (_: DateTimeParseException) {
        false
    }

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

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
