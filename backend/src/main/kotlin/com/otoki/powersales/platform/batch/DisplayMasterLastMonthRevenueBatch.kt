package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.schedule.service.DisplayMasterLastMonthRevenueBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * DisplayWorkSchedule.lastMonthRevenue daily batch 진입 클래스 (spec #690).
 *
 * legacy `UpdateLastMonthRevenueBatch.cls` (Schedulable + Batchable) 동등 복원.
 * cron 은 레거시 SF CronTrigger `UpdateLastMonthRevenueBatch` (매일 08:00 Asia/Seoul,
 * 2026-07-28 운영 CronTrigger 전수 조회 실측) 정합. 처리 로직은
 * [DisplayMasterLastMonthRevenueBatchService.runDaily] 위임.
 *
 * 이전에는 `0 0 2 * * *` (매일 02:00) 이었다 — "매출 적재 시점 의존성 회피" 목적의 결정이었으나
 * 레거시 시각 정합 요청으로 08:00 으로 환원했다. 다만 전월 매출 출처인 ORORA 월매출 적재
 * ([OroraMonthlySalesMaterializeBatch]) 가 매월 9일 12:00 이라, 9일 당일 발화(08:00) 는 그달의
 * 월매출 적재 **이전** 시점이다. 9일 이전/당일에는 직전 적재분(전월 데이터) 을 참조하며, 9일
 * 적재 이후에는 다음 날 발화부터 최신 값이 반영된다 (일 1회 재계산이라 자동 수렴).
 *
 * 전역 `@EnableScheduling` ([BatchConfig]) 은 ON — `app.batch.display.last-month-revenue.enabled=true`
 * 인 환경에서만 빈이 생성·발화한다 (기본 OFF, dev/prod 프로파일은 application.yml 에서 ON).
 */
@Component
@ConditionalOnProperty(name = ["app.batch.display.last-month-revenue.enabled"], havingValue = "true", matchIfMissing = false)
class DisplayMasterLastMonthRevenueBatch(
    private val batchService: DisplayMasterLastMonthRevenueBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.display.last-month-revenue.cron:0 0 8 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT15M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            batchService.runDaily(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "display-master-last-month-revenue-batch"
    }
}
