package com.otoki.powersales.admin.service

import com.otoki.powersales.platform.batch.ScheduledJobCatalog
import org.springframework.scheduling.config.CronTask
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.scheduling.support.CronExpression
import org.springframework.scheduling.support.ScheduledMethodRunnable
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 각 `@Scheduled` 배치의 **실제 해석된 cron 표현식**(application.yml override + `${...:default}`
 * placeholder 치환이 완료된 최종값) 을 런타임에 조회하고, 특정 시간 윈도우 내 예상 발화 횟수를 계산한다.
 *
 * [ScheduledJobCatalog] 의 `cron` 필드는 placeholder 원문(`${...:default}`) 이라 운영 override 를
 * 반영하지 못하므로, Spring 의 [ScheduledTaskHolder] 로부터 등록된 [CronTask] 의 해석된 표현식을 읽는다.
 *
 * ## 타임존
 * 컨테이너/JVM TZ 가 `Asia/Seoul` 이고 `@Scheduled` 도 그 TZ 로 발화하므로 cron 은 KST wall-clock 으로
 * 해석된다. `scheduled_job_run.started_at` 도 JVM TZ(KST) 기준 `LocalDateTime` 이라 동일 축이다.
 * 따라서 윈도우 발화 횟수는 KST `LocalDateTime` 을 그대로 [CronExpression.next] 에 넘겨 계산한다
 * (TZ 변환 없음).
 */
@Component
class ScheduledJobCronResolver(
    private val scheduledTaskHolder: ScheduledTaskHolder,
) {

    /**
     * jobName → 실제 해석된 cron 표현식 맵.
     *
     * [ScheduledTaskHolder] 에 등록된 [CronTask] 만 대상이므로, 현재 환경에서 스케줄링 활성인
     * (빈 등록된) 잡만 포함된다. 비활성 잡은 맵에 없다 (호출부에서 catalog 원문으로 fallback).
     */
    fun resolvedCronByJobName(): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (scheduledTask in scheduledTaskHolder.scheduledTasks) {
            val task = scheduledTask.task
            if (task !is CronTask) continue
            val runnable = task.runnable
            if (runnable !is ScheduledMethodRunnable) continue
            val target = runnable.target
            // catalog 의 beanType 재사용으로 JOB_NAME 매핑 (ShedLock AOP 프록시 여부와 무관하게 isInstance 매칭).
            val entry = ScheduledJobCatalog.ENTRIES.firstOrNull { it.beanType.isInstance(target) } ?: continue
            result[entry.jobName] = task.expression
        }
        return result
    }

    /**
     * `[from, to)` 반-개구간 (KST wall-clock) 내 cron 발화 횟수.
     *
     * cron 파싱 실패 시 null 을 반환한다 (예상 횟수 미산출 — 화면에서 '-' 표기).
     *
     * @param cron 해석된 cron 표현식 (placeholder 아님).
     * @param from 윈도우 시작 (포함). KST `LocalDateTime`.
     * @param to 윈도우 끝 (제외). KST `LocalDateTime`.
     */
    fun expectedFireCount(cron: String, from: LocalDateTime, to: LocalDateTime): Int? {
        val expression = try {
            CronExpression.parse(cron)
        } catch (e: IllegalArgumentException) {
            return null
        }
        var count = 0
        // next(cursor) 는 cursor 이후 첫 발화를 준다. from 정각 발화를 포함하려면 from 직전부터 시작.
        var cursor: LocalDateTime = from.minusNanos(1)
        while (count <= MAX_FIRE_COUNT) {
            val next = expression.next(cursor) ?: break
            if (!next.isBefore(to)) break
            count++
            cursor = next
        }
        return count
    }

    companion object {
        /** 방어적 상한 — 초 단위 cron 등으로 무한 루프가 되지 않도록. 24시간 윈도우면 매분 잡도 1440 이내. */
        const val MAX_FIRE_COUNT = 100_000
    }
}
