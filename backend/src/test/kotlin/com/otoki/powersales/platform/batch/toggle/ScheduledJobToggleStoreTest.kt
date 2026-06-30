package com.otoki.powersales.platform.batch.toggle

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.env.MockEnvironment

@DisplayName("ScheduledJobToggleStore 테스트")
class ScheduledJobToggleStoreTest {

    private val JOB = "sap-outbox-worker"
    private val KEY = "scheduled-job:enabled:$JOB"

    /** dev 프로파일 환경 (토글 동작). */
    private fun devEnv() = MockEnvironment().apply { setActiveProfiles("dev") }

    /** 비대상(local) 프로파일 환경 (토글 미동작). */
    private fun localEnv() = MockEnvironment().apply { setActiveProfiles("local") }

    private fun store(template: StringRedisTemplate?, env: MockEnvironment = devEnv()) =
        ScheduledJobToggleStore(template, env)

    @Test
    @DisplayName("키 부재 - 활성(true)")
    fun absentKeyIsEnabled() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.get(KEY) } returns null

        assertThat(store(template).isEnabled(JOB)).isTrue()
    }

    @Test
    @DisplayName("값 'false' - 비활성(false)")
    fun falseValueIsDisabled() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.get(KEY) } returns "false"

        assertThat(store(template).isEnabled(JOB)).isFalse()
    }

    @Test
    @DisplayName("Redis 장애 - 활성(true) 으로 fallback")
    fun redisFailureFallsBackToEnabled() {
        val template: StringRedisTemplate = mockk()
        every { template.opsForValue() } throws RedisConnectionFailureException("down")

        assertThat(store(template).isEnabled(JOB)).isTrue()
    }

    @Test
    @DisplayName("redisTemplate 미등록(null) - 항상 활성")
    fun nullTemplateIsEnabled() {
        assertThat(store(null).isEnabled(JOB)).isTrue()
    }

    @Test
    @DisplayName("setEnabled(false) - 'false' 값 기록")
    fun setDisabledWritesFalse() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.set(KEY, "false") } returns Unit

        store(template).setEnabled(JOB, false)

        verify { ops.set(KEY, "false") }
    }

    @Test
    @DisplayName("setEnabled(true) - 키 삭제 (기본 활성 환원)")
    fun setEnabledDeletesKey() {
        val template: StringRedisTemplate = mockk()
        every { template.delete(KEY) } returns true

        store(template).setEnabled(JOB, true)

        verify { template.delete(KEY) }
    }

    @Test
    @DisplayName("setEnabled - redisTemplate null 이면 IllegalStateException")
    fun setEnabledWithoutRedisThrows() {
        assertThrows<IllegalStateException> {
            store(null).setEnabled(JOB, false)
        }
    }

    @Test
    @DisplayName("setEnabled - Redis 장애 시 IllegalStateException 전파")
    fun setEnabledRedisFailureThrows() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.set(KEY, "false") } throws RedisConnectionFailureException("down")

        assertThrows<IllegalStateException> {
            store(template).setEnabled(JOB, false)
        }
    }

    @Test
    @DisplayName("비대상 프로파일(local) - Redis 'false' 라도 항상 활성")
    fun nonTargetProfileAlwaysEnabled() {
        val template: StringRedisTemplate = mockk()
        // 비대상 프로파일이면 Redis 조회 자체를 하지 않으므로 stub 불필요.

        assertThat(store(template, localEnv()).isEnabled(JOB)).isTrue()
    }

    @Test
    @DisplayName("비대상 프로파일(local) - setEnabled 는 IllegalStateException")
    fun nonTargetProfileSetThrows() {
        val template: StringRedisTemplate = mockk()

        assertThrows<IllegalStateException> {
            store(template, localEnv()).setEnabled(JOB, false)
        }
    }

    @Test
    @DisplayName("getAllStates - 카탈로그 전 잡의 상태 맵 반환")
    fun getAllStatesCoversCatalog() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.get(any()) } answers {
            if (firstArg<String>() == KEY) "false" else null
        }

        val states = store(template).getAllStates()

        assertThat(states[JOB]).isFalse()
        assertThat(states.values.count { !it }).isEqualTo(1)
    }
}
