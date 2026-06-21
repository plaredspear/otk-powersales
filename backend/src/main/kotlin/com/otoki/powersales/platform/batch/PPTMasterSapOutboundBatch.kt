package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.promotion.service.PPTMasterSapBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 전문행사조 마스터 SAP 송신 daily cron (Spec #765).
 *
 * 매일 정오 12:00 KST (`0 0 12 * * *`) 1회 발화. 레거시 운영 CronTrigger "전문행사조 SAP 송신 배치"
 * (`0 0 12 ? * 1-7` Asia/Seoul) 와 정합 (운영 실측, 2026-06-18 확인).
 *   (정정: 이전 `0 34 * * * ?` 매시간 발화는 운영 실측과 달랐음 — 레거시는 하루 1회 정오.)
 *
 * - `ScheduledJobRunner` 위임으로 실행 이력은 `scheduled_job_run` 테이블에 자동 적재
 * - `@SchedulerLock` 으로 다중 인스턴스 환경에서 중복 발화 방지
 * - 본문은 [PPTMasterSapBatchService.runDaily] 에 위임 (TeamMemberScheduleSapOutboundBatch 패턴 정합)
 */
@Component
class PPTMasterSapOutboundBatch(
    private val pptMasterSapBatchService: PPTMasterSapBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.sap.outbound.ppt-master.cron:0 0 12 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            pptMasterSapBatchService.runDaily(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "ppt-master-sap-batch"
    }
}
