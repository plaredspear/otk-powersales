package com.otoki.powersales.platform.common.config

import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

/**
 * ShedLock 통합 테스트 (Spec #545).
 *
 * 본 프로젝트는 Testcontainers 를 도입하지 않았으므로, JdbcTemplateLockProvider 를
 * H2(PostgreSQL 호환 모드) 위에 직접 구동하여 표 4.4 의 락 파라미터가 의도대로
 * 동작하는지 검증한다. ShedLock 의 `@SchedulerLock` AOP 결합부는 라이브러리가
 * 자체 테스트로 보장하므로 본 테스트는 LockProvider 계약 검증에 집중한다.
 */
@DisplayName("SchedulerLock LockProvider 통합 테스트")
class SchedulerLockIntegrationTest {

    private lateinit var dataSource: DataSource
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        // H2 in PostgreSQL compatibility mode — 동일 인스턴스를 두 LockProvider 가 공유
        dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:shedlock_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )
        jdbcTemplate = JdbcTemplate(dataSource)
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS powersales")
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS powersales.shedlock (
                name        VARCHAR(64)                 NOT NULL,
                lock_until  TIMESTAMP WITH TIME ZONE    NOT NULL,
                locked_at   TIMESTAMP WITH TIME ZONE    NOT NULL,
                locked_by   VARCHAR(255)                NOT NULL,
                PRIMARY KEY (name)
            )
            """.trimIndent()
        )
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS powersales.shedlock")
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS powersales")
    }

    private fun newProvider(): JdbcTemplateLockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .withTableName("powersales.shedlock")
                .usingDbTime()
                .build()
        )

    private fun lockConfig(
        name: String = "pptMaster.syncValid",
        lockAtMostFor: Duration = Duration.ofMinutes(30),
        lockAtLeastFor: Duration = Duration.ofMinutes(1)
    ) = LockConfiguration(Instant.now(), name, lockAtMostFor, lockAtLeastFor)

    @Nested
    @DisplayName("동시 호출 차단 - 두 인스턴스가 동일 잡을 동시 트리거")
    inner class ConcurrentAcquisition {

        @Test
        @DisplayName("동시 트리거 시 한 인스턴스만 락 획득 -> 비즈니스 로직 1회 실행")
        fun onlyOneInstanceAcquiresLock() {
            val instanceA = newProvider()
            val instanceB = newProvider()
            val config = lockConfig()

            val readyLatch = CountDownLatch(2)
            val startGate = CountDownLatch(1)
            val businessExecutionCount = AtomicInteger(0)
            val executor = Executors.newFixedThreadPool(2)

            try {
                listOf(instanceA, instanceB).forEach { provider ->
                    executor.submit {
                        readyLatch.countDown()
                        startGate.await()
                        val acquired = provider.lock(config)
                        if (acquired.isPresent) {
                            try {
                                businessExecutionCount.incrementAndGet()
                            } finally {
                                acquired.get().unlock()
                            }
                        }
                    }
                }

                readyLatch.await(5, TimeUnit.SECONDS)
                startGate.countDown()
                executor.shutdown()
                executor.awaitTermination(10, TimeUnit.SECONDS)
            } finally {
                executor.shutdownNow()
            }

            assertThat(businessExecutionCount.get())
                .`as`("동일 잡 동시 호출 시 비즈니스 로직은 정확히 1회만 실행되어야 함")
                .isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("락 보유 중 다른 인스턴스 차단")
    inner class WhileLocked {

        @Test
        @DisplayName("인스턴스A 락 보유 중 - 인스턴스B 락 획득 실패")
        fun secondInstanceCannotAcquireWhileFirstHolds() {
            val instanceA = newProvider()
            val instanceB = newProvider()
            val config = lockConfig()

            val locked = instanceA.lock(config)
            assertThat(locked).isPresent

            val attemptB = instanceB.lock(config)
            assertThat(attemptB)
                .`as`("다른 인스턴스가 락 보유 중에는 락 획득이 차단되어야 함")
                .isEmpty

            locked.get().unlock()
        }

        @Test
        @DisplayName("락 정상 해제 후 다른 인스턴스 재획득 성공 - lockAtLeastFor 경과 후")
        fun secondInstanceAcquiresAfterLockReleased() {
            val instanceA = newProvider()
            val instanceB = newProvider()
            // lockAtLeastFor 가 0 이어야 즉시 재획득 가능 (기본 1분이면 unlock 후에도 lock_until 이 즉시 단축되지 않음)
            val config = lockConfig(lockAtLeastFor = Duration.ZERO)

            val locked = instanceA.lock(config)
            assertThat(locked).isPresent
            locked.get().unlock()

            val attemptB = instanceB.lock(config)
            assertThat(attemptB)
                .`as`("락 해제 후에는 다른 인스턴스가 정상적으로 락을 재획득해야 함")
                .isPresent
            attemptB.get().unlock()
        }
    }

    @Nested
    @DisplayName("락 만료 후 재획득")
    inner class LockExpiration {

        @Test
        @DisplayName("lockAtMostFor 경과 - 다른 인스턴스가 만료된 락 강탈 가능")
        fun expiredLockIsReclaimedByOtherInstance() {
            val instanceA = newProvider()
            val instanceB = newProvider()
            // lockAtMostFor 를 매우 짧게 설정하여 만료 시나리오 강제
            val shortLock = lockConfig(
                lockAtMostFor = Duration.ofMillis(100),
                lockAtLeastFor = Duration.ZERO
            )

            val locked = instanceA.lock(shortLock)
            assertThat(locked).isPresent
            // 의도적으로 unlock 하지 않고 만료 대기 (JVM crash 시나리오 모사)

            Thread.sleep(300)

            val attemptB = instanceB.lock(shortLock)
            assertThat(attemptB)
                .`as`("lockAtMostFor 경과 후 다른 인스턴스가 락을 획득할 수 있어야 함")
                .isPresent
            attemptB.get().unlock()
        }
    }

    @Nested
    @DisplayName("서로 다른 잡 이름 - 독립 락")
    inner class DistinctLockNames {

        @Test
        @DisplayName("서로 다른 name 의 잡 - 동시 락 획득 가능")
        fun differentJobsAcquireIndependently() {
            val instanceA = newProvider()
            val instanceB = newProvider()
            val syncJob = lockConfig(name = "pptMaster.syncValid")
            val expireJob = lockConfig(name = "pptMaster.expire")

            val syncLock = instanceA.lock(syncJob)
            val expireLock = instanceB.lock(expireJob)

            assertThat(syncLock).`as`("syncValid 잡 락 획득").isPresent
            assertThat(expireLock).`as`("expire 잡 락 획득").isPresent

            syncLock.get().unlock()
            expireLock.get().unlock()
        }
    }
}
