package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.batch.config.JMartCoordinateProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

@DisplayName("JMartCoordinateService - 요일별 좌표 보정 (레거시 Batch_JMartLatLong 동등)")
class JMartCoordinateServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val properties = JMartCoordinateProperties() // 레거시 기본값(1015773, 수=양구/금=원통)
    private val service = JMartCoordinateService(accountRepository, properties)

    @Test
    @DisplayName("수요일이면 양구점 좌표로 덮어쓴다")
    fun appliesWednesdayCoordinate() {
        val account = Account(externalKey = "1015773")
        every { accountRepository.findByExternalKey("1015773") } returns account

        val result = service.applyTodayCoordinate(DayOfWeek.WEDNESDAY)

        assertThat(result.applied).isTrue()
        assertThat(account.latitude).isEqualTo("38.101772")
        assertThat(account.longitude).isEqualTo("127.988819")
    }

    @Test
    @DisplayName("매칭 요일이 아니면(월요일) 아무 것도 하지 않는다")
    fun noopOnUnscheduledDay() {
        val result = service.applyTodayCoordinate(DayOfWeek.MONDAY)

        assertThat(result.applied).isFalse()
        // 거래처 조회 자체를 하지 않음 (mock 미설정이어도 호출 없으면 통과)
    }

    @Test
    @DisplayName("대상 거래처가 없으면 applied=false")
    fun falseWhenAccountMissing() {
        every { accountRepository.findByExternalKey("1015773") } returns null

        val result = service.applyTodayCoordinate(DayOfWeek.FRIDAY)

        assertThat(result.applied).isFalse()
        assertThat(result.label).isEqualTo("원통점")
    }
}
