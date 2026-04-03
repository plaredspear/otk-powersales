package com.otoki.internal.schedule.integration

import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Orora WorkReport Mock 구현
 *
 * 실 Orora API 연동 전까지 사용하는 Mock.
 * 항상 성공 응답을 반환하고, TeamMemberSchedule의 commuteLogId + 안전점검 데이터를 직접 업데이트한다.
 * (실서비스에서는 Orora→Salesforce 동기화로 자동 반영)
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = ["orora.mock.enabled"], havingValue = "true", matchIfMissing = true)
class OroraApiServiceMock(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) : OroraApiService {

    @Transactional
    override fun sendWorkReport(request: OroraWorkReportRequest): OroraWorkReportResult {
        // Mock: commuteLogId + 안전점검 데이터를 TeamMemberSchedule에 직접 업데이트 (Orora→SF 동기화 시뮬레이션)
        teamMemberScheduleRepository.updateCommuteLogId(request.scheduleId, "OK")
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
