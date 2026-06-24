package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.claim.service.AdminLogisticsClaimMasterSyncTestService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * SF 물류 클레임 상태 업데이트(`IF_SendLogisticsClaimToPWS`) 주기 sync 배치.
 *
 * 1시간마다 SF 변경 물류클레임 마스터를 fetch 하여 각 레코드의 pwrskey(=suggestion_id) 로 신규 제안을
 * 찾아 조치 계열 6필드를 갱신한다. 클레임 마스터 sync([ClaimMasterSyncBatch]) 와 SF 호출이 겹치지 않도록
 * 매시 30분에 실행한다. 분산 환경 중복 실행은 ShedLock 으로 차단하고, 실행 이력은 [ScheduledJobRunner] 가
 * `scheduled_job_run` 에 영속화한다.
 */
@Component
@Profile("!local")
class LogisticsClaimMasterSyncBatch(
    private val syncService: AdminLogisticsClaimMasterSyncTestService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = CRON)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT55M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            syncService.sync(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "logisticsClaimMaster.sync"
        // 매시 30분 (1시간 주기, 클레임 sync 와 오프셋). application.yml override 가능.
        const val CRON = "\${app.batch.logistics-claim-master.sync.cron:0 30 * * * *}"
    }
}
