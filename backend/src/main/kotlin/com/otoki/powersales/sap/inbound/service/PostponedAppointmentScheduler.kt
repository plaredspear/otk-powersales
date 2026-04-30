package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.schedule.repository.AppointmentRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class PostponedAppointmentScheduler(
    private val employeeRepository: EmployeeRepository,
    private val appointmentRepository: AppointmentRepository,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(
        name = "sap.processPostponedAppointments",
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT1M"
    )
    @Transactional
    fun processPostponedAppointments() {
        scheduledJobRunner.run("sap.processPostponedAppointments") { context ->
            processPostponedAppointments(LocalDate.now(), context)
        }
    }

    internal fun processPostponedAppointments(today: LocalDate, context: ScheduledJobRunContext? = null) {
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
                val appointment = appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc(employee.employeeCode)
                if (appointment == null) {
                    log.warn("예약 발령 Appointment 없음: employeeCode={}", employee.employeeCode)
                    employee.crmWorkStartDate = null
                    skippedCount++
                    continue
                }

                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, today, codeMap
                )
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
