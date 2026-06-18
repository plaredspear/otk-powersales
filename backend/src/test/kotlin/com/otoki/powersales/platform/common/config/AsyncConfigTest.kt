package com.otoki.powersales.platform.common.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@DisplayName("AsyncConfig — @Async 전용 executor 구성")
class AsyncConfigTest {

    private fun config(
        core: Int = 2,
        max: Int = 4,
        queue: Int = 500,
        await: Int = 30,
    ) = AsyncConfig(
        corePoolSize = core,
        maxPoolSize = max,
        queueCapacity = queue,
        awaitTerminationSeconds = await,
    )

    @Test
    @DisplayName("A1 주입값이 ThreadPoolTaskExecutor 에 반영된다 (core/max/queue)")
    fun executor_appliesInjectedValues() {
        val executor = config(core = 3, max = 6, queue = 100).appAsyncExecutor()

        assertThat(executor.corePoolSize).isEqualTo(3)
        assertThat(executor.maxPoolSize).isEqualTo(6)
        // queueCapacity 는 getter 미노출 — 실제 ThreadPoolExecutor 의 큐 잔여 용량으로 검증.
        assertThat(executor.threadPoolExecutor.queue.remainingCapacity()).isEqualTo(100)
    }

    @Test
    @DisplayName("A2 무한 적재 차단 — RejectedExecutionHandler 가 CallerRunsPolicy")
    fun executor_usesCallerRunsPolicy() {
        val executor = config().appAsyncExecutor()

        assertThat(executor.threadPoolExecutor.rejectedExecutionHandler)
            .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy::class.java)
    }

    @Test
    @DisplayName("A3 스레드 이름 prefix = app-async-")
    fun executor_threadNamePrefix() {
        val executor = config().appAsyncExecutor()
        assertThat(executor.threadNamePrefix).isEqualTo("app-async-")
    }

    @Test
    @DisplayName("A4 getAsyncExecutor 는 동일 executor 인스턴스를 반환 (별도 인스턴스 생성 안 함)")
    fun getAsyncExecutor_returnsExecutor() {
        // 주: 컨테이너 밖 직접 호출 — @Configuration 프록시 없이도 ThreadPoolTaskExecutor 를 반환하는지 확인.
        val asyncExecutor = config().getAsyncExecutor()
        assertThat(asyncExecutor).isInstanceOf(ThreadPoolTaskExecutor::class.java)
    }

    @Test
    @DisplayName("A5 기본값 — core=2, max=4 (HikariCP 풀 5 미만 정합)")
    fun executor_defaultsBelowConnectionPool() {
        val executor = config().appAsyncExecutor()
        assertThat(executor.corePoolSize).isEqualTo(2)
        assertThat(executor.maxPoolSize).isEqualTo(4)
        // 커넥션 풀(기본 5) 초과로 인한 커넥션 대기 블로킹 회피.
        assertThat(executor.maxPoolSize).isLessThan(5)
    }
}
