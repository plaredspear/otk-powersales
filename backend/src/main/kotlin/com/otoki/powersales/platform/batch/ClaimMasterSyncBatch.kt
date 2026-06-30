package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.claim.service.AdminClaimMasterSyncTestService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * SF 클레임 상태 업데이트(`IF_SendClaimToPWS`) 주기 sync 배치.
 *
 * 1시간마다 SF 변경 클레임 마스터를 fetch 하여 각 레코드의 pwrskey(=claim_id) 로 신규 claim 을 찾아
 * 조치/상담 6필드를 갱신한다. 분산 환경 중복 실행은 ShedLock 으로 차단하고, 실행 이력은
 * [ScheduledJobRunner] 가 `scheduled_job_run` 에 영속화한다.
 *
 * `app.batch.claim-master.sync.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF).
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.claim-master.sync.enabled"], havingValue = "true", matchIfMissing = false)
class ClaimMasterSyncBatch(
    private val syncService: AdminClaimMasterSyncTestService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = CRON)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT55M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            syncService.sync(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "claimMaster.sync"
        // 매시 정각 (1시간 주기). application.yml override 가능.
        const val CRON = "\${app.batch.claim-master.sync.cron:0 0 * * * *}"
    }
}
