package com.otoki.powersales.batch

import com.otoki.powersales.agreement.service.AgreementWordCycleService
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AgreementWordCycleBatch(
    private val service: AgreementWordCycleService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(AgreementWordCycleBatch::class.java)

    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            val result = service.runCycle()
            log.info(
                "AGREEMENT_WORD_CYCLE_BATCH branch={} resetCount={}",
                result.branch, result.resetCount
            )
            ctx.metadata(
                mapOf(
                    "branch" to result.branch.name,
                    "resetCount" to result.resetCount,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "agreement-word-cycle-batch"
    }
}
