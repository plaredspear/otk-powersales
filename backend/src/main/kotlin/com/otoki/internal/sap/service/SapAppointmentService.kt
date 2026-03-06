package com.otoki.internal.sap.service

import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.sap.dto.SapAppointmentRequest
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.Appointment
import com.otoki.internal.sap.repository.AppointmentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SapAppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val userRepository: UserRepository
) : SapSyncService<SapAppointmentRequest.ReqItem> {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun sync(items: List<SapAppointmentRequest.ReqItem>): SapSyncResult {
        val employeeIds = userRepository.findAllEmployeeIds().toSet()
        var successCount = 0
        val errors = mutableListOf<SapSyncError>()

        items.forEachIndexed { index, item ->
            try {
                syncItem(item, employeeIds)
                successCount++
            } catch (e: Exception) {
                log.warn("발령 동기화 실패: index={}, employeeCode={}, error={}",
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

    private fun syncItem(item: SapAppointmentRequest.ReqItem, employeeIds: Set<String>) {
        val employeeCode = item.employeeCode
            ?: throw IllegalArgumentException("employee_code is required")
        val appointDate = item.appointDate
            ?: throw IllegalArgumentException("appoint_date is required")

        val appointment = Appointment(
            employeeCode = employeeCode,
            empCodeExist = employeeIds.contains(employeeCode),
            afterOrgCode = item.afterOrgCode,
            afterOrgName = item.afterOrgName,
            jikchak = item.jikchak,
            jikwee = item.jikwee,
            jikgub = item.jikgub,
            workType = item.workType,
            manageType = item.manageType,
            jobCode = item.jobCode,
            workArea = item.workArea,
            jikjong = item.jikjong,
            appointDate = appointDate,
            jobName = item.jobName,
            ordDetailCode = item.ordDetailCode,
            ordDetailNode = item.ordDetailNode
        )

        appointmentRepository.save(appointment)
    }
}
