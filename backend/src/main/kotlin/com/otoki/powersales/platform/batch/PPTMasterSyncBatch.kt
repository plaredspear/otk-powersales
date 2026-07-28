package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.promotion.service.PPTMasterBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.ppt-master.sync.enabled"], havingValue = "true", matchIfMissing = false)
class PPTMasterSyncBatch(
    private val pptMasterBatchService: PPTMasterBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    // 매일 01:00 KST — 레거시 SF CronTrigger "금일 전문행사조 변경" (매일 01:00 Asia/Seoul,
    // 2026-07-28 운영 CronTrigger 전수 조회 실측) 주기 정합.
    // 전날 23:30 마감 배치(PPTMasterExpireBatch)가 소속을 해제한 뒤 그 다음 발화로 잔여 유효 마스터를
    // 재정합하는 순서 의존이 있으므로 (PPTMasterBatchService.expireMasters 주석 참조) 마감 시각 이후로 둔다.
    // 처리 로직은 today 기준 멱등 (이미 정합된 사원은 skip) 이라 일 1회 발화로 충분하다.
    // JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            pptMasterBatchService.syncValidMasters(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "pptMaster.syncValid"
    }
}
