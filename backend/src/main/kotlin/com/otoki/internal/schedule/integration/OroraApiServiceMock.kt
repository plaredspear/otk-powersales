package com.otoki.internal.schedule.integration

import com.otoki.internal.schedule.repository.ScheduleRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Orora WorkReport Mock 구현
 *
 * 실 Orora API 연동 전까지 사용하는 Mock.
 * 항상 성공 응답을 반환하고, Schedule.commuteLogId를 'OK'로 직접 업데이트한다.
 * (실서비스에서는 Orora→Salesforce 동기화로 자동 반영)
 */
@Service
@ConditionalOnProperty(name = ["orora.mock.enabled"], havingValue = "true", matchIfMissing = true)
class OroraApiServiceMock(
    private val scheduleRepository: ScheduleRepository
) : OroraApiService {

    @Transactional
    override fun sendWorkReport(scheduleSfid: String, reason: String?): OroraWorkReportResult {
        // Mock: Schedule.commuteLogId를 'OK'로 직접 업데이트 (Orora→SF 동기화 시뮬레이션)
        scheduleRepository.updateCommuteLogId(scheduleSfid, "OK")

        return OroraWorkReportResult(
            resultCode = "200",
            resultMessage = "SUCCESS"
        )
    }
}
