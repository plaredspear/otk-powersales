package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * ORORA 월별 마감 → `monthly_sales_history` 적재 daily batch (Spec #855).
 *
 * legacy `IF_REST_ORORA_ReceiveMonthlySalesHistory` (ORORA pull, ABC/Ship 마감) 동등.
 * SAP PO 어댑터 callout → ORORA DB 직조회. 레거시 SF CronTrigger "오로라 월별 매출 이력 수신"
 * `0 0 11 ? * 5#1` (매월 **첫째 주 목요일** 11:00 Asia/Seoul) — 익월 초(1~7일)에 실행되어 **전월**
 * 마감분을 적재했으며 (`OroraYearMonth__mdt` 전월값) 운영 DB 적재 분포도 이를 확인 → 본 batch 는
 * 동일 cron 으로 **전월** 동적 산출. 처리 로직은 [OroraSalesMaterializeFacade.materializeMonthly] 위임.
 *
 * 현재 [BatchConfig] 의 `@EnableScheduling` 전면 임시 비활성 (commit d927d1bc, 2026-05-14) — 본 batch
 * fire 도 비활성 상태와 무관. 어노테이션 한 줄 복원 시 즉시 활성.
 * `app.batch.orora.monthly.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF).
 */
@Component
@ConditionalOnProperty(name = ["app.batch.orora.monthly.enabled"], havingValue = "true", matchIfMissing = false)
class OroraMonthlySalesMaterializeBatch(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    // 레거시 SF CronTrigger "오로라 월별 매출 이력 수신" `0 0 11 ? * 5#1` (매월 첫째 주 목요일 11:00 Asia/Seoul) 정합.
    // Quartz 요일 5=목요일 → Spring THU. JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "\${app.batch.orora.monthly.cron:0 0 11 ? * THU#1}")
    // lockAtMostFor=PT2H: 다중 인스턴스 환경에서 거래처 chunk(≈50개) 누적 처리가 길어져도
    // 락이 본문 완료 전 만료되어 다른 노드가 중복 실행되는 것을 차단.
    // 첫 운영 실행의 실측 소요시간(ORORA_MONTHLY_MATERIALIZE elapsedMs 로그) 기반으로 추후 조정.
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT2H", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            val result = facade.materializeMonthly()
            ctx.metadata(
                mapOf(
                    "trigger" to "scheduled",
                    "salesMonth" to result.salesMonth,
                    "fetchedCount" to result.fetchedCount,
                    "upsertedCount" to result.upsertedCount,
                    "skippedAccountUnmatchedCount" to result.skippedAccountUnmatchedCount,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "orora-monthly-sales-materialize-batch"
    }
}
