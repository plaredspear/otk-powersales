package com.otoki.powersales.promotion.service

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.promotion.repository.PPTMasterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PPTMasterBatchService(
    private val pptMasterRepository: PPTMasterRepository,
    private val employeeRepository: EmployeeRepository,
    private val adminPPTMasterService: AdminPPTMasterService,
) {

    @Transactional
    fun syncValidMasters(context: ScheduledJobRunContext? = null) {
        val today = LocalDate.now()
        val validMasters = pptMasterRepository.findValidMasters(today)

        val employeeIds = validMasters.mapNotNull { it.employeeId }.distinct()
        val employeeMap = employeeRepository.findAllById(employeeIds).associateBy { it.id }

        var updated = 0
        for (master in validMasters) {
            val employee = employeeMap[master.employeeId] ?: continue
            if (employee.professionalPromotionTeam != master.teamType) {
                adminPPTMasterService.updateEmployeeTeam(employee, master.teamType)
                updated++
            }
        }
        context?.metadata(mapOf("scanned" to validMasters.size, "updated" to updated))
    }

    @Transactional
    fun expireMasters(context: ScheduledJobRunContext? = null) {
        val today = LocalDate.now()
        val expiringMasters = pptMasterRepository.findExpiringMasters(today)

        val employeeIds = expiringMasters.mapNotNull { it.employeeId }.distinct()

        var reverted = 0
        for (employeeId in employeeIds) {
            val remainingValid = pptMasterRepository.findValidMastersByEmployeeId(employeeId, today)
                .filter { it.endDate == null || it.endDate!! > today }
            if (remainingValid.isEmpty()) {
                val employee = employeeRepository.findById(employeeId).orElse(null) ?: continue
                if (employee.professionalPromotionTeam != null) {
                    adminPPTMasterService.updateEmployeeTeam(employee, null)
                    reverted++
                }
            }
        }
        context?.metadata(mapOf("expiringEmployees" to employeeIds.size, "reverted" to reverted))
    }
}
