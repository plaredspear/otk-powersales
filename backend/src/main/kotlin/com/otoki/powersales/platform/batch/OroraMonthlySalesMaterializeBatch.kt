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
 * `0 0 11 ? * 5#1` (매월 첫째 주 목요일 11:00 Asia/Seoul) 은 익월 초 1회 실행이 기본이었지만,
 * 실제 운영은 전월 재마감(정산/반품 조정) 발생 시 `OroraYearMonth__mdt` 를 전월로 되돌려 배치를
 * **수동 재실행**하는 관행이 있었다 (2026-07-22~23 SF AsyncApexJob/IF_Log 실측 — 6월분 소급 재적재).
 * 신규는 이 수동 관행을 **매주 목요일 + 매월 3일 전월 재적재**로 자동 흡수한다 — `external_key` 멱등
 * upsert 라 반복 실행이 무해하고, 재마감이 익월 언제 발생하든 다음 주기 발화에서 자동 반영된다.
 * 목요일은 레거시 CronTrigger 정합, 3일은 익월 초 최초 적재 시점(레거시 운영 분포) 보존.
 * ORORA view SELECT 부하를 고려해 매일이 아닌 주기 실행으로 운영.
 *
 * cron 이 2개인 이유: Spring [org.springframework.scheduling.support.CronExpression] 은 유닉스 cron 과
 * 달리 day-of-month 와 day-of-week 를 동시에 지정하면 **AND(교집합)** 로 해석하므로 (`0 30 11 3 * THU`
 * = "3일이면서 목요일"), "매주 목요일 또는 매월 3일" 은 단일 표현식으로 쓸 수 없다. 두 cron 이 같은
 * 시각에 겹치는 날(목요일인 3일)은 [SchedulerLock] 이 한쪽만 실행시킨다.
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

    // 기본값 매주 목요일 + 매월 3일 11:30 KST (전월 재적재) — 일별 batch (기본 매일 11:00) 와 30분 간격을 두어
    // ORORA 부하 분산. 레거시 첫째 목요일 1회 + 수동 재실행 관행을 주기 재적재로 대체 (2개 cron 사유는 클래스 주석 참조).
    // 실 운영은 환경변수 `app.batch.orora.monthly.cron`/`day3-cron` override — 본 변경 적용 시 운영값도 함께 조정 필요.
    // JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "\${app.batch.orora.monthly.cron:0 30 11 * * THU}")
    @Scheduled(cron = "\${app.batch.orora.monthly.day3-cron:0 30 11 3 * *}")
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
