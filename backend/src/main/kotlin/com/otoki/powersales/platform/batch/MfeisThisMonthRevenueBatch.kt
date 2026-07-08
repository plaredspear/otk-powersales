package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.schedule.service.MfeisThisMonthRevenueBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * MFEIS `this_month_amount` 월간 일괄 갱신 batch 진입 클래스 (spec #680 §5.2).
 *
 * legacy `UpdateThisMonthRevenueBatch.cls` (Schedulable + Batchable) 동등 복원. cron 사용자 결정
 * (#680 Q15 옵션 1): `0 0 3 1 * ?` 매월 1일 03:00 (전월 마감 + 일 경과 후 안정 시점).
 * ShedLock (#680 Q13 옵션 1): chunk 200 + `lockAtMostFor=PT30M / lockAtLeastFor=PT5M`.
 * 처리 로직은 [MfeisThisMonthRevenueBatchService.runMonthly] 위임.
 *
 * 전역 `@EnableScheduling` ([BatchConfig]) 은 ON — `app.batch.mfeis.this-month-revenue.enabled=true`
 * 인 환경에서만 빈이 생성·발화한다 (기본 OFF, dev/prod 프로파일은 application.yml 에서 ON).
 */
@Component
@ConditionalOnProperty(name = ["app.batch.mfeis.this-month-revenue.enabled"], havingValue = "true", matchIfMissing = false)
class MfeisThisMonthRevenueBatch(
    private val batchService: MfeisThisMonthRevenueBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.mfeis.this-month-revenue.cron:0 0 3 1 * ?}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            batchService.runMonthly(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "mfeis-this-month-revenue-batch"
    }
}
