package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PostponedAppointmentBatchService(
    private val employeeRepository: EmployeeRepository,
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
                // SF PostponedAppointmentBatch.cls:15 정합 — 반영 대상은 예약 시 기록한 발령 참조
                // (PostponedAppointment__c 대응) 로만 특정한다. SF 는 start 쿼리에서
                // `PostponedAppointment__c != null` 로 참조 없는 건을 아예 배제하므로, 사원코드 기준
                // "최신 발령" 을 추측하는 경로를 두지 않는다.
                //
                // 참조 없이 crm_work_start_date 만 있는 건은 SF 에서도 영구 미반영 상태인 잔여 예약이다
                // (운영 실측: SF 38건 중 36건이 참조 null). 추측으로 반영하면 동일 발령일 다건에서 옛
                // 발령을 집어 인사정보를 되돌리는 사고가 난다 — 실제로 직무코드 4건이 옛 값으로 회귀했다.
                val appointment = employee.postponedAppointment
                if (appointment == null) {
                    log.warn("예약 발령 참조 없음 — 예약 해제만 수행: employeeCode={}", employee.employeeCode)
                    employee.crmWorkStartDate = null
                    skippedCount++
                    continue
                }

                // SF PostponedAppointmentBatch.cls:102 정합 — 사원의 발령일자는 배치 실행일이 아니라
                // 발령 레코드의 발령일이다. 실행일을 넣으면 예약분이 반영될 때마다 발령일이 그날로
                // 덮여, 인사 이력상 존재하지 않는 날짜가 남는다.
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, appointment.appointDate ?: today, codeMap
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
