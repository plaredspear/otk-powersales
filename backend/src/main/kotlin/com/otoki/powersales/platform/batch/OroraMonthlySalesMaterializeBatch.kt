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
 * (매월 9일 12:00 Asia/Seoul — 2026-07-28 운영 CronTrigger 전수 조회 실측) 의 주기를 그대로 따른다.
 *
 * 레거시 운영에는 전월 재마감(정산/반품 조정) 발생 시 `OroraYearMonth__mdt` 를 전월로 되돌려 배치를
 * **수동 재실행**하는 관행이 있었다 (2026-07-22~23 SF AsyncApexJob/IF_Log 실측 — 6월분 소급 재적재).
 * 신규도 이 관행을 그대로 이어받는다 — 9일 이후 재마감이 발생하면 다음 달 발화까지 자동 반영되지
 * 않으므로 관리 화면 수동 실행으로 처리한다 (`external_key` 멱등 upsert 라 반복 실행 무해).
 * ORORA view SELECT 부하를 고려해 매일이 아닌 월 1회 실행으로 운영.
 * 처리 로직은 [OroraSalesMaterializeFacade.materializeMonthly] 위임 (**전월** 동적 산출).
 *
 * 전역 `@EnableScheduling` ([BatchConfig]) 은 ON — `app.batch.orora.monthly.enabled=true` 인 환경에서만
 * 빈이 생성·발화한다 (기본 OFF, dev/prod 프로파일은 application.yml 에서 ON).
 */
@Component
@ConditionalOnProperty(name = ["app.batch.orora.monthly.enabled"], havingValue = "true", matchIfMissing = false)
class OroraMonthlySalesMaterializeBatch(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    // 기본값 매월 9일 11:30 KST — 레거시 SF CronTrigger "오로라 월별 매출 이력 수신"
    // `0 0 12 9 */1 ?` (매월 9일 12:00 Asia/Seoul) 의 발화일 정합.
    // 실 운영 발화 시각은 application.yml `app.batch.orora.monthly.cron` (매월 9일 12:00 KST) 고정값이
    // override — 환경변수 설정 없이 yml 만으로 전체 스케줄이 동작한다. 스케줄 변경은 코드 배포로만.
    // JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "\${app.batch.orora.monthly.cron:0 30 11 9 * *}")
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
