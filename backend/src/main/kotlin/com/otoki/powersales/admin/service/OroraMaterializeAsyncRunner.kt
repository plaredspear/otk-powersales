package com.otoki.powersales.admin.service

import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import com.otoki.powersales.platform.batch.OroraDailySalesMaterializeBatch
import com.otoki.powersales.platform.batch.OroraMonthlySalesMaterializeBatch
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * ORORA 매출 수동 적재를 **비동기**(`@Async`)로 실행하는 러너.
 *
 * ## 도입 배경
 * ORORA view 의 무거운 chunk SELECT 는 수십 초~수 분 걸릴 수 있다. 수동 적재를 동기(HTTP 요청 처리
 * 스레드)로 실행하면 그 시간 내내 HTTP 커넥션이 열려 있어야 하고, 프론트/ALB idle timeout 이 먼저
 * 끊으면 브라우저에는 504 로 보이면서 서버 적재는 계속 도는 혼란이 생긴다. 이를 없애기 위해 컨트롤러는
 * 즉시 "접수(accepted)" 응답을 돌려주고, 실제 적재는 본 러너의 별도 스레드에서 수행한다.
 *
 * ## 이력 기록
 * 각 메서드는 스케줄 배치와 **동일한 JOB_NAME** 으로 [ScheduledJobRunner.run] 으로 감싼다 —
 * `scheduled_job_run` 에 RUNNING → SUCCESS/FAILURE 이력이 남아, 개발자 도구 > 스케줄 잡 이력 탭에서
 * 진행/결과를 조회한다 (비동기라 응답으로 결과를 받을 수 없으므로 이력이 유일한 결과 확인 경로).
 * 예외는 runner 가 FAILURE 로 기록 후 rethrow 하며, `@Async` void 메서드라 최종적으로
 * [com.otoki.powersales.platform.common.config.AsyncConfig] 의 uncaught handler(로깅)로 흡수된다.
 *
 * ## 스레드 / executor
 * 기본 `@Async` executor(`appAsyncExecutor`) 를 재사용한다. facade / chunk processor 는 로그인 principal
 * 에 의존하지 않으므로(정적 거래처 범위 + 명시 월) SecurityContext 전파 없이 안전하다.
 *
 * ## 동시 중복 실행
 * 별도 분산 락은 부착하지 않는다 (운영자 단발 호출 — 중복 실행은 운영자 책임). upsert 는 멱등이라
 * 중복 실행이 데이터를 깨지 않는다.
 */
@Component
class OroraMaterializeAsyncRunner(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** ORORA 월매출 전체 범위 비동기 적재. `salesMonth` null 이면 전월 자동 산출. */
    @Async("appAsyncExecutor")
    fun runMonthly(salesMonth: String?) {
        scheduledJobRunner.run(OroraMonthlySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.materializeMonthly(salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual",
                    "salesMonth" to r.salesMonth,
                    "fetchedCount" to r.fetchedCount,
                    "upsertedCount" to r.upsertedCount,
                    "skippedAccountUnmatchedCount" to r.skippedAccountUnmatchedCount,
                )
            )
            log.info(
                "[ORORA_MONTHLY_MANUAL] 비동기 적재 완료 salesMonth={} fetched={} upserted={} unmatched={}",
                r.salesMonth, r.fetchedCount, r.upsertedCount, r.skippedAccountUnmatchedCount,
            )
        }
    }

    /** ORORA 월매출 거래처 청크 1개 비동기 적재. */
    @Async("appAsyncExecutor")
    fun runMonthlyChunk(chunkIndex: Int, salesMonth: String?) {
        scheduledJobRunner.run(OroraMonthlySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.materializeMonthlyChunk(chunkIndex, salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual-chunk",
                    "chunkIndex" to chunkIndex,
                    "salesMonth" to r.salesMonth,
                    "fetchedCount" to r.fetchedCount,
                    "upsertedCount" to r.upsertedCount,
                    "skippedAccountUnmatchedCount" to r.skippedAccountUnmatchedCount,
                )
            )
            log.info(
                "[ORORA_MONTHLY_MANUAL] 비동기 청크 적재 완료 chunkIndex={} salesMonth={} fetched={} upserted={}",
                chunkIndex, r.salesMonth, r.fetchedCount, r.upsertedCount,
            )
        }
    }

    /** ORORA 일매출 전체 범위 비동기 적재. `salesMonth` null 이면 당월 자동 산출. */
    @Async("appAsyncExecutor")
    fun runDaily(salesMonth: String?) {
        scheduledJobRunner.run(OroraDailySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.materializeDaily(salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual",
                    "salesMonth" to r.salesMonth,
                    "dailyUpsertedCount" to r.dailyUpsertedCount,
                    "monthlyAggregateUpdatedCount" to r.monthlyAggregateUpdatedCount,
                )
            )
            log.info(
                "[ORORA_DAILY_MANUAL] 비동기 적재 완료 salesMonth={} dailyUpserted={} monthlyAggregateUpdated={}",
                r.salesMonth, r.dailyUpsertedCount, r.monthlyAggregateUpdatedCount,
            )
        }
    }

    /** ORORA 일매출 거래처 청크 1개 비동기 적재. */
    @Async("appAsyncExecutor")
    fun runDailyChunk(chunkIndex: Int, salesMonth: String?) {
        scheduledJobRunner.run(OroraDailySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.materializeDailyChunk(chunkIndex, salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual-chunk",
                    "chunkIndex" to chunkIndex,
                    "salesMonth" to r.salesMonth,
                    "dailyUpsertedCount" to r.dailyUpsertedCount,
                    "monthlyAggregateUpdatedCount" to r.monthlyAggregateUpdatedCount,
                )
            )
            log.info(
                "[ORORA_DAILY_MANUAL] 비동기 청크 적재 완료 chunkIndex={} salesMonth={} dailyUpserted={}",
                chunkIndex, r.salesMonth, r.dailyUpsertedCount,
            )
        }
    }

    /**
     * 월별 합계 재집계(전체 거래처 범위) 비동기 실행 — ORORA 조회 없이 daily 전량 SUM 으로 월합계 정합.
     * daily 배치와 동일 JOB_NAME 으로 이력에 남기되 metadata `trigger=manual-reaggregate` 로 구분한다.
     */
    @Async("appAsyncExecutor")
    fun runMonthlyReaggregate(salesMonth: String) {
        scheduledJobRunner.run(OroraDailySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.reaggregateMonthly(salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual-reaggregate",
                    "salesMonth" to r.salesMonth,
                    "monthlyAggregateUpdatedCount" to r.monthlyAggregateUpdatedCount,
                )
            )
            log.info(
                "[MONTHLY_REAGGREGATE_MANUAL] 비동기 재집계 완료 salesMonth={} monthlyAggregateUpdated={}",
                r.salesMonth, r.monthlyAggregateUpdatedCount,
            )
        }
    }

    /** 월별 합계 재집계 거래처 청크 1개 비동기 실행. */
    @Async("appAsyncExecutor")
    fun runMonthlyReaggregateChunk(chunkIndex: Int, salesMonth: String) {
        scheduledJobRunner.run(OroraDailySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.reaggregateMonthlyChunk(chunkIndex, salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual-reaggregate-chunk",
                    "chunkIndex" to chunkIndex,
                    "salesMonth" to r.salesMonth,
                    "monthlyAggregateUpdatedCount" to r.monthlyAggregateUpdatedCount,
                )
            )
            log.info(
                "[MONTHLY_REAGGREGATE_MANUAL] 비동기 청크 재집계 완료 chunkIndex={} salesMonth={} monthlyAggregateUpdated={}",
                chunkIndex, r.salesMonth, r.monthlyAggregateUpdatedCount,
            )
        }
    }
}
