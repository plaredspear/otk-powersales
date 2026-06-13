package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.foundation.account.service.AccountNaverGeocodeService
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AccountNaverGeocodeBatch(
    private val service: AccountNaverGeocodeService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(AccountNaverGeocodeBatch::class.java)

    @Scheduled(cron = CRON)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            val result = service.enrichCoordinatesMissingAccounts(BATCH_SIZE)
            log.info(
                "ACCOUNT_NAVER_GEOCODE_BATCH scanned={} succeeded={} failed={}",
                result.scanned, result.succeeded, result.failed
            )
            ctx.metadata(
                mapOf(
                    "scanned" to result.scanned,
                    "succeeded" to result.succeeded,
                    "failed" to result.failed,
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
