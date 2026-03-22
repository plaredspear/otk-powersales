package com.otoki.internal.sap.service

import com.otoki.internal.sap.repository.AppointmentRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class PostponedAppointmentScheduler(
    private val employeeRepository: EmployeeRepository,
    private val appointmentRepository: AppointmentRepository,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun processPostponedAppointments() {
        processPostponedAppointments(LocalDate.now())
    }

    internal fun processPostponedAppointments(today: LocalDate) {
        val employees = employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today)
        if (employees.isEmpty()) {
            log.info("예약 발령 대상 없음")
            return
        }

        log.info("예약 발령 대상: {}명", employees.size)
        val codeMap = appointmentUserProfileUpdater.loadSystemCodeMap()

        var processedCount = 0
        var skippedCount = 0

        for (employee in employees) {
            try {
                val appointment = appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc(employee.employeeNumber)
                if (appointment == null) {
                    log.warn("예약 발령 Appointment 없음: employeeNumber={}", employee.employeeNumber)
                    employee.crmWorkStartDate = null
                    skippedCount++
                    continue
                }

                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, today, codeMap
                )
                processedCount++
            } catch (e: Exception) {
                log.warn("예약 발령 처리 실패: employeeNumber={}, error={}", employee.employeeNumber, e.message)
                skippedCount++
            }
        }

        log.info("예약 발령 처리 완료: processed={}, skipped={}", processedCount, skippedCount)
    }
}
