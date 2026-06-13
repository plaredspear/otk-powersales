package com.otoki.powersales.platform.batch

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.promotion.service.PPTMasterSapBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 전문행사조 마스터 SAP 송신 hourly cron (Spec #765).
 *
 * 매시간 34분 0초 KST (`0 34 * * * ?`) 마다 발화. 레거시 SF Scheduled Jobs 의
 * `IF_REST_SAP_PPTMToSAP` Apex 클래스 cron 표현식 1:1 정합 (운영 실측, 2026-05-18 확인).
 *
 * - `ScheduledJobRunner` 위임으로 실행 이력은 `scheduled_job_run` 테이블에 자동 적재
 * - `@SchedulerLock` 으로 다중 인스턴스 환경에서 중복 발화 방지
 * - 본문은 [PPTMasterSapBatchService.runHourly] 에 위임 (AttendanceSapOutboundBatch 패턴 정합)
 */
@Component
class PPTMasterSapOutboundBatch(
    private val pptMasterSapBatchService: PPTMasterSapBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.sap.outbound.ppt-master.cron:0 34 * * * ?}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            pptMasterSapBatchService.runHourly(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "ppt-master-sap-batch"
    }
}
