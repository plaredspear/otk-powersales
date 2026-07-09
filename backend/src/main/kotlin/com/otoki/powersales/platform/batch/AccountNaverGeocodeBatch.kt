package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.foundation.account.service.AccountNaverGeocodeService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.batch.account-naver-geocode.enabled"], havingValue = "true", matchIfMissing = false)
class AccountNaverGeocodeBatch(
    private val service: AccountNaverGeocodeService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(AccountNaverGeocodeBatch::class.java)

    @Scheduled(cron = CRON)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            val result = service.enrichCoordinatesMissingAccounts(BATCH_SIZE)
            log.info(
                "ACCOUNT_NAVER_GEOCODE_BATCH scanned={} succeeded={} unresolved={} callFailed={}",
                result.scanned, result.succeeded, result.unresolved, result.callFailed
            )
            ctx.metadata(
                mapOf(
                    "scanned" to result.scanned,
                    "succeeded" to result.succeeded,
                    // 영구 실패(주소 못 찾음) — 다음 배치부터 재조회 제외.
                    "unresolved" to result.unresolved,
                    // 일시 실패(HTTP/네트워크 오류) — 다음 배치 재시도 대상 유지.
                    "callFailed" to result.callFailed,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "account-naver-geocode-batch"
        const val CRON = "0 0 2 * * *"
        private const val BATCH_SIZE = 1000
    }
}
