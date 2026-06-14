package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.repository.AppointmentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PostponedAppointmentBatchService(
    private val employeeRepository: EmployeeRepository,
    private val appointmentRepository: AppointmentRepository,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(context: ScheduledJobRunContext? = null) {
        process(LocalDate.now(), context)
    }

    @Transactional
    internal fun process(today: LocalDate, context: ScheduledJobRunContext? = null) {
        val employees = employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today)
        if (employees.isEmpty()) {
            log.info("예약 발령 대상 없음")
            context?.metadata(mapOf("processed" to 0, "skipped" to 0))
            return
        }

        log.info("예약 발령 대상: {}명", employees.size)
        val codeMap = appointmentUserProfileUpdater.loadSystemCodeMap()

        var processedCount = 0
        var skippedCount = 0

        for (employee in employees) {
            try {
                val appointment = employee.employeeCode
                    ?.let { appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc(it) }
                if (appointment == null) {
                    log.warn("예약 발령 Appointment 없음: employeeCode={}", employee.employeeCode)
                    employee.crmWorkStartDate = null
                    skippedCount++
                    continue
                }

                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, today, codeMap
                )
                appointmentUserProfileUpdater.updateUserProfileCache(employee)
                processedCount++
            } catch (e: Exception) {
                log.warn("예약 발령 처리 실패: employeeCode={}, error={}", employee.employeeCode, e.message)
                skippedCount++
            }
        }

        log.info("예약 발령 처리 완료: processed={}, skipped={}", processedCount, skippedCount)
        context?.metadata(mapOf("processed" to processedCount, "skipped" to skippedCount))
    }
}
