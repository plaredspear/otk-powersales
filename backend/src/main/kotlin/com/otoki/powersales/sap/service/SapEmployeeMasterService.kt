package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.sap.dto.SapEmployeeMasterRequest
import com.otoki.powersales.sap.dto.SapSyncError
import com.otoki.powersales.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class SapEmployeeMasterService(
    private val employeeRepository: EmployeeRepository
) : SapSyncService<SapEmployeeMasterRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    private val statusMap = mapOf(
        "1" to "재직",
        "2" to "휴직",
        "3" to "퇴직"
    )

    @Transactional
    override fun sync(items: List<SapEmployeeMasterRequest.ReqItem>): SapSyncResult {
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncItem(item)
                successCount++
            } catch (e: Exception) {
                log.warn("사원마스터 동기화 실패: index={}, employeeCode={}, error={}",
                    index, item.employeeCode, e.message)
                errors.add(
                    SapSyncError(
                        index = index,
                        field = "employee_code",
                        value = item.employeeCode,
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

    private fun syncItem(item: SapEmployeeMasterRequest.ReqItem) {
        val employeeCode = item.employeeCode
            ?: throw IllegalArgumentException("employee_code is required")
        val employeeName = item.employeeName
            ?: throw IllegalArgumentException("employee_name is required")
        val statusCode = item.status
            ?: throw IllegalArgumentException("status is required")

        val statusName = statusMap[statusCode]
            ?: throw IllegalArgumentException("Unknown status code: $statusCode")

        val isActive = resolveAppLoginActive(statusCode, item.lockingFlag)
        val startDate = parseDate(item.startDate)

        val existingEmployee = employeeRepository.findByEmployeeCode(employeeCode).orElse(null)

        val sex = when (item.sex) {
            "1" -> "남"
            "2" -> "여"
            else -> null
        }
        val endDate = parseDate(item.endDate)

        if (existingEmployee != null) {
            updateEmployee(existingEmployee, employeeName, statusName, isActive, item, startDate, sex, endDate)
            employeeRepository.save(existingEmployee)
        } else {
            val newEmployee = Employee(
                employeeCode = employeeCode,
                name = employeeName,
                status = statusName,
                appLoginActive = isActive,
                birthDate = item.birthdate,
                homePhone = item.homePhone,
                workPhone = item.workPhone,
                costCenterCode = item.orgCode,
                startDate = startDate,
                sex = sex,
                endDate = endDate,
                passwordChangeRequired = true
            )
            employeeRepository.save(newEmployee)
        }
    }

    private fun updateEmployee(
        employee: Employee,
        name: String,
        status: String,
        isActive: Boolean,
        item: SapEmployeeMasterRequest.ReqItem,
        startDate: LocalDate?,
        sex: String?,
        endDate: LocalDate?
    ) {
        employee.name = name
        employee.status = status
        employee.appLoginActive = isActive
        employee.birthDate = item.birthdate
        employee.homePhone = item.homePhone
        employee.workPhone = item.workPhone
        employee.costCenterCode = item.orgCode
        employee.startDate = startDate
        employee.sex = sex
        employee.endDate = endDate
    }

    private fun resolveAppLoginActive(statusCode: String, lockingFlag: String?): Boolean {
        if (statusCode != "1") return false
        if (lockingFlag == "Y") return false
        return true
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return LocalDate.parse(dateStr, dateFormatter)
    }
}
