package com.otoki.powersales.domain.activity.schedule.attendance

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.repository.AttendanceLogRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 출근 등록 실 구현.
 *
 * 출근 등록의 본질은 `attendance_log` row 생성 + `team_member_schedule.attendance_log_id` 백링크다.
 * 레거시 SF `IF_REST_MOBILE_WorkReport` (CommuteLog__c insert + TeamMemberSchedule__c 연결) 에 대응한다.
 *
 * 레거시 대비 필드 적재:
 * - `attendance_date` = 등록 시각 (레거시 `DKRetail__CommuteDate__c = system.now()` 동등, IF_REST_MOBILE_WorkReport.cls:78).
 * - `reason` = 요청 사유 (레거시 `DKRetail__Reason__c = inputObj.Reason` 동등, cls:77).
 * - `employee_id` / `account_id` / `attendance_type` = 신규 id-FK 모델 실체 데이터.
 *   레거시 CommuteLog__c 는 이 값들을 NULL 로 두고 TeamMemberSchedule 측에 두었으나, 신규는 출근로그
 *   자체에 적재해 admin 출근현황 조회(AdminAttendanceLog*)와 정합시킨다.
 * - `owner_user_id` / `created_by` 등 audit 컬럼은 [com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener]
 *   + JPA auditing 이 자동 세팅 (레거시 OwnerId/CreatedById = 연동 계정 동등).
 *
 * SAP 등 외부 전송은 본 시점에 없으며, SAP 역전송은 별도 일배치가 담당한다 (`attendance_log` 연결 여부가
 * 익일 SAP 재전송 대상 선정 조건 — 레거시 Batch_TeamMemberSchedule 동등).
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = ["attendance.registrar.mock.enabled"], havingValue = "false", matchIfMissing = true)
class AttendanceRegistrarImpl(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val attendanceLogRepository: AttendanceLogRepository,
    private val clock: Clock,
) : AttendanceRegistrar {

    @Transactional
    override fun register(request: AttendanceRegisterRequest): AttendanceRegisterResult {
        // 1. attendance_log row INSERT — 실체 데이터 적재 (출근일시=등록 시각, 사원/거래처/출근종류/사유).
        val savedLog = attendanceLogRepository.save(
            AttendanceLog(
                employeeId = request.employeeId,
                accountId = request.accountId,
                attendanceDate = LocalDateTime.now(clock),
                attendanceType = request.attendanceType,
                reason = request.reason,
            )
        )

        // 2. TMS attendance_log id-FK 백링크 (Spec #789 — id-FK 가드 기준).
        teamMemberScheduleRepository.updateAttendanceLog(request.scheduleId, savedLog.id)

        // 3. TMS 안전점검 데이터 반영.
        teamMemberScheduleRepository.updateSafetyCheckData(
            id = request.scheduleId,
            equipment1 = request.equipment1,
            equipment2 = request.equipment2,
            equipment3 = request.equipment3,
            equipment4 = request.equipment4,
            equipment5 = request.equipment5,
            equipment6 = request.equipment6,
            equipment7 = request.equipment7,
            equipment8 = request.equipment8,
            equipment9 = request.equipment9,
            yesChkCnt = request.yesCount?.toDouble(),
            noChkCnt = request.noCount?.toDouble(),
            startTime = request.startTime?.let { LocalDateTime.parse(it) },
            completeTime = request.completeTime?.let { LocalDateTime.parse(it) },
            precaution = request.precaution,
            precautionChk = request.precautionCount?.toDouble(),
            traversalFlag = request.traversalFlag
        )

        return AttendanceRegisterResult(
            resultCode = "200",
            resultMessage = "SUCCESS"
        )
    }
}
