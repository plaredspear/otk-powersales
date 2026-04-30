package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.entity.Gender
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterRequestItem
import com.otoki.powersales.sap.inbound.dto.employee.FailureItem
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.sap.repository.SystemCodeMasterRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * SAP 직원 마스터 인바운드 UPSERT 서비스. (Spec #557)
 *
 * - UPSERT 키: [Employee.employeeCode] (= 페이로드 EmployeeCode)
 * - Status 코드 변환: SystemCodeMaster (group=H10010, company=1000) 매칭 시 코드명, 매칭 실패 시 원본
 * - Sex / Date / LockingFlag 변환 규칙은 레거시 `IF_REST_SAP_EmployeeMaster` 와 동일
 * - 부분 실패 허용 (행 단위 검증 후 saveAll 일괄)
 * - INSERT 시 `Employee.employeeInfo` 가 cascade=ALL 로 자동 영속화됨
 */
@Service
class SapEmployeeMasterService(
    private val employeeRepository: EmployeeRepository,
    private val systemCodeMasterRepository: SystemCodeMasterRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<EmployeeMasterRequestItem>): EmployeeMasterDetail {
        val employeeCodes = items.mapNotNull { it.employeeCode?.takeIf { code -> code.isNotBlank() } }
        val cache: MutableMap<String, Employee> = if (employeeCodes.isEmpty()) {
            mutableMapOf()
        } else {
            employeeRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .associateBy { it.employeeCode }
                .toMutableMap()
        }

        val statusCodeMap: Map<String, String> = systemCodeMasterRepository
            .findByGroupCodeIn(listOf(STATUS_GROUP_CODE))
            .asSequence()
            .filter { it.companyCode == STATUS_COMPANY_CODE }
            .mapNotNull { entry ->
                val name = entry.detailCodeName ?: return@mapNotNull null
                entry.detailCode to name
            }
            .toMap()

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<Employee>()

        items.forEach { item ->
            try {
                val employeeCode = item.employeeCode?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("EmployeeCode 필수")
                val employeeName = item.employeeName?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("EmployeeName 필수")

                val convertedGender = Gender.fromSapCode(item.gender)
                val startDate = parseDate(item.startDate, "StartDate")
                val endDate = parseDate(item.endDate, "EndDate")
                val birthDate = normalizeBirthdate(item.birthdate)
                val resolvedStatus = item.status?.let { statusCodeMap[it] ?: it }
                val appLoginActive = item.lockingFlag != "Y"

                val entity = cache[employeeCode]?.also {
                    applyMutableFields(it, item, employeeName, convertedGender, startDate, endDate, birthDate, resolvedStatus, appLoginActive)
                } ?: Employee(employeeCode = employeeCode, name = employeeName).also {
                    applyMutableFields(it, item, employeeName, convertedGender, startDate, endDate, birthDate, resolvedStatus, appLoginActive)
                    cache[employeeCode] = it
                }
                toSave += entity
            } catch (ex: IllegalArgumentException) {
                failures += FailureItem(item.employeeCode, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            employeeRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)

        return EmployeeMasterDetail(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun applyMutableFields(
        entity: Employee,
        item: EmployeeMasterRequestItem,
        name: String,
        gender: Gender?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        birthDate: String?,
        status: String?,
        appLoginActive: Boolean
    ) {
        entity.name = name
        entity.gender = gender
        entity.homePhone = item.homePhone
        entity.workPhone = item.workPhone
        entity.workEmail = item.workEmail
        entity.email = item.email
        entity.startDate = startDate
        entity.endDate = endDate
        entity.status = status
        entity.birthDate = birthDate
        entity.costCenterCode = item.orgCode
        entity.appLoginActive = appLoginActive
    }

    private fun parseDate(value: String?, fieldName: String): LocalDate? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw == EMPTY_DATE) return null
        return try {
            LocalDate.parse(raw, DATE_FORMATTER)
        } catch (ex: DateTimeParseException) {
            throw IllegalArgumentException("$fieldName 형식 오류")
        }
    }

    private fun normalizeBirthdate(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw == EMPTY_DATE) return null
        return raw
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
        private const val STATUS_GROUP_CODE = "H10010"
        private const val STATUS_COMPANY_CODE = "1000"
        private const val EMPTY_DATE = "00000000"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
