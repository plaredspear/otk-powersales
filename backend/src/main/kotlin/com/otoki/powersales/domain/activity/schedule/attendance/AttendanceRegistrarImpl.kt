package com.otoki.powersales.domain.activity.schedule.attendance

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.repository.AttendanceLogRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 출근 등록 실 구현.
 *
 * `attendance_log` row 생성만 담당한다. TMS 백링크·안전점검 stamp 는 호출측(AttendanceService)이
 * managed entity 에 직접 반영한다 ([AttendanceRegistrar] 문서 참조 — bulk UPDATE 백링크는
 * 이후 dirty flush 에 덮여 유실됐던 이력이 있다).
 *
 * 레거시 대비 필드 적재:
 * - `attendance_date` = 등록 시각 (레거시 `DKRetail__CommuteDate__c = system.now()` 동등, IF_REST_MOBILE_WorkReport.cls:78).
 * - `reason` = 요청 사유 (레거시 `DKRetail__Reason__c = inputObj.Reason` 동등, cls:77).
 * - `employee_id` / `account_id` / `attendance_type` = 신규 id-FK 모델 실체 데이터.
 *   레거시 CommuteLog__c 는 이 값들을 NULL 로 두고 TeamMemberSchedule 측에 두었으나, 신규는 출근로그
 *   자체에 적재해 admin 출근현황 조회(AdminAttendanceLog*)와 정합시킨다.
 * - `owner_user_id` / `created_by` 등 audit 컬럼은 [com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener]
 *   + JPA auditing 이 자동 세팅 (레거시 OwnerId/CreatedById = 연동 계정 동등).
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = ["attendance.registrar.mock.enabled"], havingValue = "false", matchIfMissing = true)
class AttendanceRegistrarImpl(
    private val attendanceLogRepository: AttendanceLogRepository,
    private val clock: Clock,
) : AttendanceRegistrar {

    @Transactional
    override fun register(request: AttendanceRegisterRequest): AttendanceLog {
        return attendanceLogRepository.save(
            AttendanceLog(
                employeeId = request.employeeId,
                accountId = request.accountId,
                attendanceDate = LocalDateTime.now(clock),
                attendanceType = request.attendanceType,
                reason = request.reason,
            )
        )
    }
}
