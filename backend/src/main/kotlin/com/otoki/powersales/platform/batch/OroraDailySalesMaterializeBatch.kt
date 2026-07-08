package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * ORORA 일별 매출 → `daily_sales_history` 적재 + 월별 합계 갱신 daily batch (Spec #855).
 *
 * legacy `Schedule_OroraDailySalesHistory_M1` → `Batch/Queueable_OroraDailySalesHistory_M1` (ORORA pull) 동등.
 * SAP PO 어댑터 callout → ORORA DB 직조회. 대상 월은 당월 동적 산출 (Q2). 처리 로직은
 * [OroraSalesMaterializeFacade.materializeDaily] 위임.
 *
 * 전역 `@EnableScheduling` ([BatchConfig]) 은 ON — `app.batch.orora.daily.enabled=true` 인 환경에서만
 * 빈이 생성·발화한다 (기본 OFF, dev/prod 프로파일은 application.yml 에서 ON). ORORA DataSource 가
 * dev/prod 한정이라 local/test 에서는 fire 되어도 ORORA 호출 site 가 graceful 처리.
 */
@Component
@ConditionalOnProperty(name = ["app.batch.orora.daily.enabled"], havingValue = "true", matchIfMissing = false)
class OroraDailySalesMaterializeBatch(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    // 기본값은 레거시 SF CronTrigger "오로라 일별 데이터 수신" `0 0 11 ? * 1,2,3,4,5,6,7` (매일 11:00 Asia/Seoul) 정합.
    // 실 운영은 application.yml `app.batch.orora.daily.cron` 이 override (현재 매일 04:30 KST).
    // JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "\${app.batch.orora.daily.cron:0 0 11 * * *}")
    // lockAtMostFor=PT2H: 다중 인스턴스 환경에서 거래처 chunk(≈50개) 누적 처리가 길어져도
    // 락이 본문 완료 전 만료되어 다른 노드가 중복 실행 → 월별 합계 정합성이 깨지는 것을 차단.
    // 첫 운영 실행의 실측 소요시간(ORORA_DAILY_MATERIALIZE elapsedMs 로그) 기반으로 추후 조정.
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT2H", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            val result = facade.materializeDaily()
            ctx.metadata(
                mapOf(
                    "trigger" to "scheduled",
                    "salesMonth" to result.salesMonth,
                    "dailyUpsertedCount" to result.dailyUpsertedCount,
                    "monthlyAggregateUpdatedCount" to result.monthlyAggregateUpdatedCount,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "orora-daily-sales-materialize-batch"
    }
}
