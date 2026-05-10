package com.otoki.powersales.agreement.batch

import com.otoki.powersales.agreement.service.AgreementWordCycleService
import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * GPS 동의문구(AgreementWord) 6개월 재동의 강제 cycle batch (스펙 #654).
 *
 * - cron: 매일 09:00 (legacy `AgreementWord_Test.cls:50` 의 `0 0 9 * * ? *` 단서 채택, Q1 legacy 동등)
 * - ShedLock: 다중 인스턴스 동시 실행 차단 (`agreement-word-cycle-batch`, Q7 — #545 인프라 재사용)
 * - ScheduledJobRunner: 실행 이력 영속화 + metadata (#548 표준 패턴)
 * - 본 클래스는 cron 진입 + 락 획득만 책임. 분기 / cascade 로직은 [AgreementWordCycleService.runCycle] 참조.
 */
@Component
class AgreementWordCycleBatch(
    private val service: AgreementWordCycleService,
    private val scheduledJobRunner: ScheduledJobRunner
) {

    private val log = LoggerFactory.getLogger(AgreementWordCycleBatch::class.java)

    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(
        name = JOB_NAME,
        lockAtMostFor = "PT30M",
        lockAtLeastFor = "PT5M"
    )
    fun runDaily() {
        scheduledJobRunner.run(JOB_NAME) { context ->
            execute(context)
        }
    }

    internal fun execute(context: ScheduledJobRunContext? = null) {
        val result = service.runCycle()
        log.info(
            "AGREEMENT_WORD_CYCLE_BATCH branch={} resetCount={}",
            result.branch, result.resetCount
        )
        context?.metadata(
            mapOf(
                "branch" to result.branch.name,
                "resetCount" to result.resetCount
            )
        )
    }

    companion object {
        const val JOB_NAME = "agreement-word-cycle-batch"
    }
}
