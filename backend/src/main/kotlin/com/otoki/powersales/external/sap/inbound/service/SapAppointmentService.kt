package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentDetail
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentRequestItem
import com.otoki.powersales.external.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.schedule.service.AppointmentInsertService
import com.otoki.powersales.schedule.service.dto.AppointmentInsertCommand
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * SAP 인사발령 인바운드 어댑터. (Spec #562 / 어댑터-도메인 분리: #635 P2-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [AppointmentRequestItem] → 도메인 커맨드 [AppointmentInsertCommand] 매핑
 * - 도메인 서비스 [AppointmentInsertService.insert] 호출 (INSERT only, 멱등성 미보장)
 * - 도메인 결과 → SAP 응답 [AppointmentDetail] 매핑
 * - 후처리 트리거 [AppointmentUserProfileUpdater.updateUserProfiles] 호출 (적재 commit 후, 실패 시 적재는 유지 + 로그)
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.external.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 */
@Service
class SapAppointmentService(
    private val appointmentInsertService: AppointmentInsertService,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @SapInboundAccepted("items")
    fun insert(items: List<AppointmentRequestItem>): AppointmentDetail {
        val commands = items.map { it.toCommand() }
        val result = appointmentInsertService.insert(commands)

        if (result.savedAppointments.isNotEmpty()) {
            try {
                appointmentUserProfileUpdater.updateUserProfiles(result.savedAppointments)
            } catch (ex: Exception) {
                log.warn("AppointmentUserProfileUpdater 실패 (적재는 유지): {}", ex.message, ex)
            }
        }

        return AppointmentDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun AppointmentRequestItem.toCommand(): AppointmentInsertCommand = AppointmentInsertCommand(
        employeeCode = employeeCode,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jikchak = jikchak,
        jikwee = jikwee,
        jikgub = jikgub,
        workType = workType,
        manageType = manageType,
        jobCode = jobCode,
        workArea = workArea,
        jikjong = jikjong,
        appointDate = appointDate,
        jobName = jobName,
        ordDetailCode = ordDetailCode,
        ordDetailNode = ordDetailNode
    )
}
