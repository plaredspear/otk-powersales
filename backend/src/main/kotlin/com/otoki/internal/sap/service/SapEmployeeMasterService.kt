package com.otoki.internal.sap.service

import com.otoki.internal.common.entity.User
import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.sap.dto.SapEmployeeMasterRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class SapEmployeeMasterService(
    private val userRepository: UserRepository
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

        val existingUser = userRepository.findByEmployeeId(employeeCode).orElse(null)

        if (existingUser != null) {
            updateUser(existingUser, employeeName, statusName, isActive, item, startDate)
            userRepository.save(existingUser)
        } else {
            val newUser = User(
                employeeId = employeeCode,
                name = employeeName,
                status = statusName,
                appLoginActive = isActive,
                birthDate = item.birthdate,
                homePhone = item.homePhone,
                workPhone = item.workPhone,
                costCenterCode = item.orgCode,
                startDate = startDate,
                passwordChangeRequired = true
            )
            userRepository.save(newUser)
        }
    }

    private fun updateUser(
        user: User,
        name: String,
        status: String,
        isActive: Boolean,
        item: SapEmployeeMasterRequest.ReqItem,
        startDate: LocalDate?
    ) {
        user.name = name
        user.status = status
        user.appLoginActive = isActive
        user.birthDate = item.birthdate
        user.homePhone = item.homePhone
        user.workPhone = item.workPhone
        user.costCenterCode = item.orgCode
        user.startDate = startDate
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
