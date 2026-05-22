package com.otoki.powersales.orora

import com.otoki.powersales.schedule.entity.AttendanceLog
import com.otoki.powersales.schedule.repository.AttendanceLogRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Orora WorkReport Mock 구현
 *
 * 실 Orora API 연동 전까지 사용하는 Mock.
 * 항상 성공 응답을 반환하고, AttendanceLog 더미 row INSERT 후 TeamMemberSchedule.attendance_log_id 백링크 + 안전점검 데이터를 업데이트한다.
 * (실서비스에서는 Orora → Salesforce 동기화 → HC sync 가 attendance_log row + team_member_schedule.commute_log_sfid 를 채움)
 *
 * Spec #789 정합: 신규 시스템 비즈니스 로직 가드는 attendance_log_id (id-FK) 기준. mock 도 동일 패턴으로 시뮬레이션.
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = ["orora.mock.enabled"], havingValue = "true", matchIfMissing = true)
class OroraApiServiceMock(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val attendanceLogRepository: AttendanceLogRepository,
) : OroraApiService {

    @Transactional
    override fun sendWorkReport(request: OroraWorkReportRequest): OroraWorkReportResult {
        // Mock: AttendanceLog 더미 row INSERT + TMS attendance_log id-FK 백링크 + 안전점검 데이터 업데이트 (Orora → SF → HC sync 시뮬레이션, Spec #789).
        val savedLog = attendanceLogRepository.save(AttendanceLog())
        teamMemberScheduleRepository.updateAttendanceLog(request.scheduleId, savedLog.id)
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

        return OroraWorkReportResult(
            resultCode = "200",
            resultMessage = "SUCCESS"
        )
    }
}
