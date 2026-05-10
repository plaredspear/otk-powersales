package com.otoki.powersales.account.batch

import com.otoki.powersales.account.service.AccountNaverGeocodeService
import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 좌표 미수신 거래처 Naver Geocode 보강 daily batch (#637).
 *
 * - cron: `app.account.naver-geocode.cron` (기본 `0 0 2 * * *`) — 매일 새벽 2시
 * - LIMIT: `app.account.naver-geocode.batch-size` (기본 1000, 레거시 동등)
 * - ShedLock 으로 동시 실행 방지 (`account-naver-geocode-batch`)
 * - 거래처별 단일 트랜잭션 (JPA dirty-checking) — 부분 실패는 다음 batch 자연 재진입
 */
@Component
class AccountNaverGeocodeBatch(
    private val service: AccountNaverGeocodeService,
    private val scheduledJobRunner: ScheduledJobRunner,
    @Value("\${app.account.naver-geocode.batch-size:1000}") private val batchSize: Int
) {

    private val log = LoggerFactory.getLogger(AccountNaverGeocodeBatch::class.java)

    @Scheduled(cron = "\${app.account.naver-geocode.cron:0 0 2 * * *}")
    @SchedulerLock(
        name = JOB_NAME,
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT1M"
    )
    fun runDaily() {
        scheduledJobRunner.run(JOB_NAME) { context ->
            execute(context)
        }
    }

    internal fun execute(context: ScheduledJobRunContext? = null) {
        val result = service.enrichCoordinatesMissingAccounts(batchSize)
        log.info(
            "ACCOUNT_NAVER_GEOCODE_BATCH scanned={} succeeded={} failed={}",
            result.scanned, result.succeeded, result.failed
        )
        context?.metadata(
            mapOf(
                "scanned" to result.scanned,
                "succeeded" to result.succeeded,
                "failed" to result.failed
            )
        )
    }

    companion object {
        const val JOB_NAME = "account-naver-geocode-batch"
    }
}
