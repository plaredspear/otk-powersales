package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.foundation.account.service.JMartCoordinateService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * J마트(이동매장) 요일별 좌표 보정 배치 (매일).
 *
 * 레거시 `Batch_JMartLatLong` (Schedulable, 요일별 좌표 덮어쓰기) 동등. 매일 실행하되 매칭 요일(기본 수/금)에만
 * 좌표가 갱신된다. 기본 cron 은 매일 03:30 (`app.batch.jmart-coordinate.cron` 으로 재정의 가능).
 *
 * ⚠️ 전역 `@EnableScheduling` (BatchConfig) 활성 시에만 실제 발화한다.
 * `app.batch.jmart-coordinate.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF).
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.jmart-coordinate.enabled"], havingValue = "true", matchIfMissing = false)
class JMartCoordinateBatch(
    private val jMartCoordinateService: JMartCoordinateService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.jmart-coordinate.cron:0 30 3 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            val result = jMartCoordinateService.applyTodayCoordinate()
            ctx.metadata(
                mapOf(
                    "externalKey" to result.externalKey,
                    "applied" to result.applied,
                    "label" to result.label,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "account.jmart-coordinate"
    }
}
