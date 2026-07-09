package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * SF 클레임/물류클레임 상태 업데이트(inbound sync) 주기 배치 — 두 도메인 통합 (기본 매시 정각).
 *
 * 클레임(`IF_SendClaimToPWS`) + 물류클레임(`IF_SendLogisticsClaimToPWS`) 을 한 잡에서 순차 fetch 하여 각 레코드의
 * pwrskey 로 신규 claim / suggestion 을 찾아 조치/상담 필드를 갱신한다. 실제 도메인 오케스트레이션/격리는
 * [ClaimMasterSyncBatchService.syncAll] 이 담당한다.
 *
 * 두 도메인을 한 잡에서 순차 처리해 SF 부하를 직렬화하고 운영 이력/토글을 단일화한다. (통합 전에는 정각/30분
 * 오프셋으로 SF 호출을 벌려놨으나, 순차 처리로 오프셋이 불필요해졌다.)
 *
 * 잡 빈은 `app.batch.claim-master.sync.enabled=true` 로 게이팅되며, claim / logistics **도메인별 개별 on/off** 는
 * [ClaimMasterSyncBatchService] 가 두 플래그(`claim-master.sync.enabled` / `logistics-claim-master.sync.enabled`)로
 * 다시 판정한다 — 기존 분리 제어를 그대로 보존한다. 분산 환경 중복 실행은 ShedLock 으로 차단하고, 실행 이력은
 * [ScheduledJobRunner] 가 `scheduled_job_run` 에 영속화한다.
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.claim-master.sync.enabled"], havingValue = "true", matchIfMissing = false)
class ClaimMasterSyncBatch(
    private val syncBatchService: ClaimMasterSyncBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = CRON)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT55M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            syncBatchService.syncAll(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "claimMaster.sync"
        // 매시 정각 (1시간 주기). application.yml override 가능.
        const val CRON = "\${app.batch.claim-master.sync.cron:0 0 * * * *}"
    }
}
