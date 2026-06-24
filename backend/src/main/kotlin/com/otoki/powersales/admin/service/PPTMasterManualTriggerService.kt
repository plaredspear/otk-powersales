package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.response.ScheduledJobManualTriggerResponse
import com.otoki.powersales.domain.activity.promotion.service.PPTMasterBatchService
import com.otoki.powersales.platform.batch.PPTMasterExpireBatch
import com.otoki.powersales.platform.batch.PPTMasterSyncBatch
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRun
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import org.springframework.stereotype.Service

/**
 * 운영자 화면에서 전문행사조(PPT) 마스터 배치를 수동 실행하는 서비스.
 *
 * 대상은 "금일 전문행사조 마감"([PPTMasterExpireBatch], `pptMaster.expire`) 과
 * "금일 전문행사조 반영"([PPTMasterSyncBatch], `pptMaster.syncValid`) 두 잡이다.
 *
 * 자동 스케줄 경로와 동일하게 [ScheduledJobRunner.run] 으로 감싸 `scheduled_job_run` 에 이력을
 * 남기므로, 수동 실행 결과도 화면의 이력 탭에서 그대로 조회된다. 이력 metadata 에는 통계와 함께
 * `triggeredBy=MANUAL` 이 기록되어 자동 실행과 구분된다.
 *
 * 트랜잭션 경계: 본 서비스는 클래스 레벨 트랜잭션을 두지 않는다. [ScheduledJobRunner] 가 이력
 * INSERT/UPDATE 를 `REQUIRES_NEW` 로 분리 영속하고, 본문 람다의 [PPTMasterBatchService] 메서드가
 * 자체 `@Transactional`(쓰기) 로 새 트랜잭션을 시작한다 (부모 트랜잭션 없음).
 */
@Service
class PPTMasterManualTriggerService(
    private val pptMasterBatchService: PPTMasterBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    /** 지원하는 수동 실행 대상 jobName 집합 — 컨트롤러 입력 검증에 사용. */
    fun supports(jobName: String): Boolean = jobName in SUPPORTED_JOB_NAMES

    /**
     * [jobName] 에 해당하는 전문행사조 배치를 즉시 1회 실행한다.
     *
     * 본문이 예외 없이 끝나면 `SUCCESS`, 실패 시 [ScheduledJobRunner] 가 이력에 FAILURE 를 기록한 뒤
     * 예외를 그대로 전파한다 (컨트롤러에서 에러 응답).
     *
     * @throws IllegalArgumentException 지원하지 않는 jobName
     */
    fun trigger(jobName: String): ScheduledJobManualTriggerResponse {
        require(supports(jobName)) { "수동 실행을 지원하지 않는 jobName 입니다: $jobName" }

        scheduledJobRunner.run(jobName) { ctx ->
            when (jobName) {
                PPTMasterExpireBatch.JOB_NAME -> pptMasterBatchService.expireMasters(ctx, triggeredBy = TRIGGERED_BY_MANUAL)
                PPTMasterSyncBatch.JOB_NAME -> pptMasterBatchService.syncValidMasters(ctx, triggeredBy = TRIGGERED_BY_MANUAL)
            }
        }

        return ScheduledJobManualTriggerResponse(
            jobName = jobName,
            status = ScheduledJobRun.STATUS_SUCCESS,
        )
    }

    companion object {
        const val TRIGGERED_BY_MANUAL = "MANUAL"

        val SUPPORTED_JOB_NAMES: Set<String> = setOf(
            PPTMasterExpireBatch.JOB_NAME,
            PPTMasterSyncBatch.JOB_NAME,
        )
    }
}
