package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.admin.exception.PostponedAppointmentNotFoundException
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 유예 발령 수동 승인 service — 레거시 SF `ManualConfirmPostponedAppController` +
 * Quick Action "신규발령확정" (`DKRetail__Employee__c.NewConfirmBtn`) 이관.
 *
 * ## 레거시 동작 요약
 * 1. 입력: employeeId (SF Quick Action 의 recordId 대응).
 * 2. 사원의 유예 발령 참조([Employee.postponedAppointment]) 를 **날짜 게이트 없이** 즉시 반영 —
 *    예정일이 지나 소진 배치(등호 조회)가 영영 집지 않는 예약의 유일한 구제 경로다.
 * 3. 참조 부재 시 [PostponedAppointmentNotFoundException] — SF `checkPostponedAppointment` 가
 *    모달에서 사전 차단하던 상태.
 * 4. 필드 반영은 [AppointmentUserProfileUpdater.applyManualConfirmAppointment] (SF 수동 확정
 *    전용 필드 구성 — 트리거/배치와 다른 제3 변형, 해당 KDoc 참조).
 * 5. User(Profile) 캐시 갱신 수행 — SF 가 `AppointmentTriggerHanlder.updateUser`(@future) 를
 *    호출하는 것과 동등 (소진 배치와 달리 미생략).
 *
 * ## 신규 차이
 * - SF @future(비동기, DML 후 커밋 시점) 대신 동일 트랜잭션 동기 갱신 — 트리거 경로와 동일한 기확정 동등 처리.
 */
@Service
class AdminEmployeeAppointmentConfirmService(
    private val employeeRepository: EmployeeRepository,
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun confirmPostponedAppointment(employeeId: Long): EmployeeDetailResponse {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException(employeeId)

        val appointment = employee.postponedAppointment
            ?: throw PostponedAppointmentNotFoundException(employeeId)

        val codeMap = appointmentUserProfileUpdater.loadSystemCodeMap()
        appointmentUserProfileUpdater.applyManualConfirmAppointment(employee, appointment, codeMap)
        appointmentUserProfileUpdater.updateUserProfileCache(employee)

        log.info(
            "POSTPONED_APPOINTMENT_CONFIRMED employeeId={} employeeCode={} appointmentId={} appointDate={}",
            employee.id, employee.employeeCode, appointment.id, appointment.appointDate,
        )
        return EmployeeDetailResponse.from(employee)
    }
}
