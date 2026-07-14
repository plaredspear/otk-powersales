package com.otoki.powersales.admin.service

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.scheduling.config.ScheduledTaskHolder
import java.time.LocalDateTime

@DisplayName("ScheduledJobCronResolver.expectedFireCount 테스트")
class ScheduledJobCronResolverTest {

    // resolvedCronByJobName 은 실제 ScheduledTaskHolder 등록이 필요하므로 통합 테스트 영역.
    // 여기서는 순수 계산 로직인 expectedFireCount 만 검증한다.
    private val resolver = ScheduledJobCronResolver(mockk<ScheduledTaskHolder>())

    /** 일별 실행현황 표준 윈도우: 전일 22:00 ~ 당일 22:00 (24시간). */
    private val from = LocalDateTime.of(2026, 7, 14, 22, 0, 0)
    private val to = LocalDateTime.of(2026, 7, 15, 22, 0, 0)

    @Test
    @DisplayName("매시간 정각 cron 은 24시간 윈도우에서 24회 발화")
    fun hourlyOnTheHour_24() {
        // 0 0 * * * * = 매시간 0분 0초. [22:00, 다음날 22:00) 에는 22,23,0,...,21 시 정각 = 24회.
        val count = resolver.expectedFireCount("0 0 * * * *", from, to)
        assertThat(count).isEqualTo(24)
    }

    @Test
    @DisplayName("매시간 44분 cron 은 24시간 윈도우에서 24회 발화")
    fun hourlyAt44_24() {
        val count = resolver.expectedFireCount("0 44 * * * *", from, to)
        assertThat(count).isEqualTo(24)
    }

    @Test
    @DisplayName("매일 01시 cron 은 윈도우에 정확히 1회 (7/15 01:00 포함)")
    fun dailyAt1_1() {
        val count = resolver.expectedFireCount("0 0 1 * * *", from, to)
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("윈도우 시작(22:00) 정각 발화도 포함된다 (goe 경계)")
    fun windowStartInclusive() {
        // 진열마스터 SAP전송 = 0 0 23 * * *. 윈도우 [7/14 22:00, 7/15 22:00) 에서 7/14 23:00 = 1회.
        assertThat(resolver.expectedFireCount("0 0 23 * * *", from, to)).isEqualTo(1)
        // 경계 시작 정각(22:00)에 발화하는 cron 은 포함 (from 은 goe).
        assertThat(resolver.expectedFireCount("0 0 22 * * *", from, to)).isEqualTo(1)
    }

    @Test
    @DisplayName("윈도우 끝(22:00) 정각 발화는 제외된다 (lt 경계)")
    fun windowEndExclusive() {
        // 0 0 22 * * * 는 from 의 22:00 1회만 (to 의 22:00 은 lt 라 제외) — 위 테스트와 동일하게 1회.
        val count = resolver.expectedFireCount("0 0 22 * * *", from, to)
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("ORORA 월매출(매월 3일 05시) — 3일을 포함하지 않는 윈도우는 0회")
    fun monthlyDay3_notMatchingWindow_zero() {
        // 7/14~7/15 윈도우에는 3일이 없으므로 0회.
        val count = resolver.expectedFireCount("0 0 5 3 * *", from, to)
        assertThat(count).isEqualTo(0)
    }

    @Test
    @DisplayName("ORORA 월매출(매월 3일 05시) — 3일을 포함하는 윈도우는 1회")
    fun monthlyDay3_matchingWindow_one() {
        // 8/2 22:00 ~ 8/3 22:00 윈도우에는 8/3 05:00 발화 = 1회.
        val f = LocalDateTime.of(2026, 8, 2, 22, 0, 0)
        val t = LocalDateTime.of(2026, 8, 3, 22, 0, 0)
        val count = resolver.expectedFireCount("0 0 5 3 * *", f, t)
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("파싱 불가 cron 은 null 반환")
    fun invalidCron_null() {
        val count = resolver.expectedFireCount("not-a-cron", from, to)
        assertThat(count).isNull()
    }
}
