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
 * 현재 [BatchConfig] 의 `@EnableScheduling` 전면 임시 비활성 (commit d927d1bc, 2026-05-14) — 본 batch
 * fire 도 비활성 상태와 무관. 어노테이션 한 줄 복원 시 즉시 활성. ORORA DataSource 가 dev/prod 한정이라
 * local/test 에서는 fire 되어도 ORORA 호출 site 가 graceful 처리.
 * `app.batch.orora.daily.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF).
 */
@Component
@ConditionalOnProperty(name = ["app.batch.orora.daily.enabled"], havingValue = "true", matchIfMissing = false)
class OroraDailySalesMaterializeBatch(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    // 레거시 SF CronTrigger "오로라 일별 데이터 수신" `0 0 11 ? * 1,2,3,4,5,6,7` (매일 11:00 Asia/Seoul) 정합.
    // JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "\${app.batch.orora.daily.cron:0 0 11 * * *}")
    // lockAtMostFor=PT2H: 다중 인스턴스 환경에서 거래처 chunk(≈50개) 누적 처리가 길어져도
    // 락이 본문 완료 전 만료되어 다른 노드가 중복 실행 → 월별 합계 정합성이 깨지는 것을 차단.
    // 첫 운영 실행의 실측 소요시간(ORORA_DAILY_MATERIALIZE elapsedMs 로그) 기반으로 추후 조정.
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT2H", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
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
