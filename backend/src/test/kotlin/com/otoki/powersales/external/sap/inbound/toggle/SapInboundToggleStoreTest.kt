package com.otoki.powersales.external.sap.inbound.toggle

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

@DisplayName("SapInboundToggleStore 테스트")
class SapInboundToggleStoreTest {

    private val ENDPOINT = "/api/v1/sap/account"
    private val KEY = "sap:inbound:enabled:$ENDPOINT"

    @Test
    @DisplayName("키 부재 - 활성(true)")
    fun absentKeyIsEnabled() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.get(KEY) } returns null

        assertThat(SapInboundToggleStore(template).isEnabled(ENDPOINT)).isTrue()
    }

    @Test
    @DisplayName("값 'false' - 비활성(false)")
    fun falseValueIsDisabled() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.get(KEY) } returns "false"

        assertThat(SapInboundToggleStore(template).isEnabled(ENDPOINT)).isFalse()
    }

    @Test
    @DisplayName("Redis 장애 - 활성(true) 으로 fallback")
    fun redisFailureFallsBackToEnabled() {
        val template: StringRedisTemplate = mockk()
        every { template.opsForValue() } throws RedisConnectionFailureException("down")

        assertThat(SapInboundToggleStore(template).isEnabled(ENDPOINT)).isTrue()
    }

    @Test
    @DisplayName("redisTemplate 미등록(null) - 항상 활성")
    fun nullTemplateIsEnabled() {
        assertThat(SapInboundToggleStore(null).isEnabled(ENDPOINT)).isTrue()
    }

    @Test
    @DisplayName("setEnabled(false) - 'false' 값 기록")
    fun setDisabledWritesFalse() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        every { ops.set(KEY, "false") } returns Unit

        SapInboundToggleStore(template).setEnabled(ENDPOINT, false)

        verify { ops.set(KEY, "false") }
    }

    @Test
    @DisplayName("setEnabled(true) - 키 삭제 (기본 활성 환원)")
    fun setEnabledDeletesKey() {
        val template: StringRedisTemplate = mockk()
        every { template.delete(KEY) } returns true

        SapInboundToggleStore(template).setEnabled(ENDPOINT, true)

        verify { template.delete(KEY) }
    }

    @Test
    @DisplayName("setEnabled - redisTemplate null 이면 IllegalStateException")
    fun setEnabledWithoutRedisThrows() {
        assertThrows<IllegalStateException> {
            SapInboundToggleStore(null).setEnabled(ENDPOINT, false)
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
            SapInboundToggleStore(template).setEnabled(ENDPOINT, false)
        }
    }

    @Test
    @DisplayName("getAllStates - 카탈로그 전 endpoint 의 상태 맵 반환")
    fun getAllStatesCoversCatalog() {
        val template: StringRedisTemplate = mockk()
        val ops: ValueOperations<String, String> = mockk()
        every { template.opsForValue() } returns ops
        // account 만 비활성, 나머지는 활성(키 부재)
        every { ops.get(any()) } answers {
            if (firstArg<String>() == KEY) "false" else null
        }

        val states = SapInboundToggleStore(template).getAllStates()

        assertThat(states[ENDPOINT]).isFalse()
        assertThat(states.values.count { !it }).isEqualTo(1)
    }
}
