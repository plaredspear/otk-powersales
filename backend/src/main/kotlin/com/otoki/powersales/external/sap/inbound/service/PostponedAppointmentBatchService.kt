package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@Service
class PostponedAppointmentBatchService(
    private val employeeRepository: EmployeeRepository,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater,
    transactionManager: PlatformTransactionManager,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // SF PostponedAppointmentBatch 의 Database.upsert(allOrNone=false) 행 격리 정합 — 사원마다
    // 독립 트랜잭션으로 커밋해, 한 행의 오류(로직/DB 모두)가 나머지 행의 반영을 무효화하지 않도록 한다.
    private val rowTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun process(context: ScheduledJobRunContext? = null) {
        process(LocalDate.now(), context)
    }

    internal fun process(today: LocalDate, context: ScheduledJobRunContext? = null) {
        // SF PostponedAppointmentBatch.cls:15 정합 — 발령 예정일 당일(등호) + 참조 보유 건만 조회한다.
        // 과거 일자 잔여 예약(마이그레이션 재유입 포함)은 SF 처럼 영영 건드리지 않는다 — 조건 근거는
        // EmployeeRepository.findByCrmWorkStartDateAndPostponedAppointmentIsNotNull 주석 참조.
        // 행 격리를 위해 여기서는 ID 만 확보하고, 반영은 행별 트랜잭션 안에서 재조회해 수행한다.
        val employeeIds = employeeRepository.findByCrmWorkStartDateAndPostponedAppointmentIsNotNull(today)
            .map { it.id }
        if (employeeIds.isEmpty()) {
            log.info("예약 발령 대상 없음")
            context?.metadata(mapOf("processed" to 0, "skipped" to 0))
            return
        }

        log.info("예약 발령 대상: {}명", employeeIds.size)
        val codeMap = appointmentUserProfileUpdater.loadSystemCodeMap()

        var processedCount = 0
        var skippedCount = 0

        for (employeeId in employeeIds) {
            try {
                val processed = rowTransaction.execute {
                    val employee = employeeRepository.findById(employeeId).orElse(null)
                        ?: return@execute false

                    // 조회 조건이 참조 non-null 을 보장하므로 도달 불가 방어 — SF 정합상 반영 대상은
                    // 예약 시 기록한 발령 참조(PostponedAppointment__c 대응)로만 특정하며, 사원코드 기준
                    // "최신 발령" 추측 경로는 두지 않는다 (동일 발령일 다건 오선택 사고 이력).
                    val appointment = employee.postponedAppointment
                    if (appointment == null) {
                        log.warn("예약 발령 참조 없음 — skip: employeeCode={}", employee.employeeCode)
                        return@execute false
                    }

                    // SF PostponedAppointmentBatch.cls:100-143 정합 — 배치 전용 반영 (트리거 즉시 경로와
                    // 필드 구성이 다르다: prefix 없음 / A053 미포함 / PPT 미초기화 / User 미갱신 / 예약 소진).
                    appointmentUserProfileUpdater.applyPostponedAppointment(employee, appointment, codeMap)
                    true
                }
                if (processed == true) processedCount++ else skippedCount++
            } catch (e: Exception) {
                log.warn("예약 발령 처리 실패 (행 격리 — 나머지 행 진행): employeeId={}, error={}",
                    employeeId, e.message)
                skippedCount++
            }
        }

        log.info("예약 발령 처리 완료: processed={}, skipped={}", processedCount, skippedCount)
        context?.metadata(mapOf("processed" to processedCount, "skipped" to skippedCount))
    }
}
