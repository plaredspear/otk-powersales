package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.schedule.service.MfeisThisMonthRevenueBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * MFEIS `this_month_amount` 일괄 갱신 batch 진입 클래스 (spec #680 §5.2).
 *
 * legacy `UpdateThisMonthRevenueBatch.cls` (Schedulable + Batchable) 동등 복원.
 * cron 은 레거시 SF CronTrigger `UpdateThisMonthRevenueBatch` (매일 08:05 Asia/Seoul,
 * 2026-07-28 운영 CronTrigger 전수 조회 실측) 정합.
 * ShedLock (#680 Q13 옵션 1): chunk 200 + `lockAtMostFor=PT30M / lockAtLeastFor=PT5M`.
 * 처리 로직은 [MfeisThisMonthRevenueBatchService.runMonthly] 위임.
 *
 * 이전에는 `0 0 3 1 * ?` (매월 1일 03:00, #680 Q15 옵션 1 — "전월 마감 + 일 경과 후 안정 시점")
 * 이었으나 레거시 주기 정합 요청으로 매일 발화로 환원했다. `runMonthly` 는 인자 없이 호출되면
 * `YearMonth.now().minusMonths(1)` (전월) 을 대상으로 재계산하는 멱등 연산이라 매일 실행해도
 * 결과가 동일하다 — 한 달 내내 같은 전월분을 재계산하며, 월이 바뀌면 대상도 자동 이동한다.
 * 레거시도 동일하게 매일 전월분을 재계산했다.
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

    @Scheduled(cron = "\${app.batch.mfeis.this-month-revenue.cron:0 5 8 * * *}")
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
