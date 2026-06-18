package com.otoki.powersales.platform.common.config

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 비동기 후처리 (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) 활성화 + 전용 executor 명시.
 *
 * 도입 배경: SF 레거시 `@future` 메서드 (예: `IF_REST_SAP_EmployeeMaster.upsertUser`) 동등 매핑.
 * 메인 트랜잭션 commit 이후 별도 스레드 + 별도 트랜잭션에서 부수 작업을 수행한다.
 *
 * ## executor 를 명시하는 이유
 * Spring Boot 기본 `@Async` executor 는 동작이 자동구성/버전에 암묵 의존하고, 기본 큐 용량이 사실상
 * 무제한(`Integer.MAX_VALUE`)이라 대량 후처리 유입 시 작업 객체가 큐에 무한 적재되어 메모리를 압박할 수
 * 있다. 또한 graceful shutdown 시 큐에 남은 작업이 유실될 수 있다. 이를 막기 위해:
 *
 * - **maxPoolSize 를 DB 커넥션 풀(HikariCP, 기본 5)보다 작게** 둔다. `@Async` 작업이 `REQUIRES_NEW`
 *   로 커넥션을 잡으므로, executor 동시 실행 수가 풀을 넘으면 스레드가 커넥션 대기로 블로킹 점유된다.
 * - **queueCapacity 를 유한값**으로 제한해 무한 적재를 차단한다.
 * - **CallerRunsPolicy** — 큐가 차면 호출(이벤트 발행) 스레드가 직접 실행해 배압(back-pressure)을
 *   형성, 생산 속도를 자연히 늦춘다.
 * - **graceful shutdown** — 종료 시 큐에 남은 작업 완료를 대기해 유실을 막는다.
 *
 * 값은 `app.async.*` (application.yml) 로 외부화되어 환경별 튜닝이 가능하다.
 */
@Configuration
@EnableAsync
class AsyncConfig(
    @Value("\${app.async.core-pool-size:2}") private val corePoolSize: Int,
    @Value("\${app.async.max-pool-size:4}") private val maxPoolSize: Int,
    @Value("\${app.async.queue-capacity:500}") private val queueCapacity: Int,
    @Value("\${app.async.await-termination-seconds:30}") private val awaitTerminationSeconds: Int,
) : AsyncConfigurer {

    /**
     * `@Async` 메서드의 전용 executor (`app-async-*` 스레드).
     *
     * 빈으로도 노출하여 `@Async("appAsyncExecutor")` 명시 지정 + 진단(메트릭) 접근이 가능하다.
     */
    @Bean(name = ["appAsyncExecutor"])
    fun appAsyncExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = this@AsyncConfig.corePoolSize
            maxPoolSize = this@AsyncConfig.maxPoolSize
            queueCapacity = this@AsyncConfig.queueCapacity
            setThreadNamePrefix("app-async-")
            // 큐가 차면 호출 스레드가 직접 실행 — 무한 적재 차단 + 배압 형성.
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            // 종료 시 진행 중/대기 작업 완료 대기 — 큐에 남은 후처리 유실 방지.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(this@AsyncConfig.awaitTerminationSeconds)
            initialize()
        }

    override fun getAsyncExecutor(): Executor = appAsyncExecutor()

    /**
     * `@Async` 메서드(void 반환)에서 던져진 예외의 최종 핸들러.
     *
     * 후처리 리스너는 자체 try/catch 로 예외를 삼키지만(SF @future 동등), 누락 대비 안전망으로
     * executor 레벨에서도 기본 핸들러(로깅)를 둔다.
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        SimpleAsyncUncaughtExceptionHandler()
}
