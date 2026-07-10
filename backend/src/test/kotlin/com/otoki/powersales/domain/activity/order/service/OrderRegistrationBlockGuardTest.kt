package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.exception.OrderRegistrationBlockedException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

@DisplayName("OrderRegistrationBlockGuard 테스트 (운영 임시 차단)")
class OrderRegistrationBlockGuardTest {

    @Test
    @DisplayName("prod 프로파일이 활성이면 assertNotBlocked 가 차단 예외를 던진다")
    fun blocksOnProd() {
        val guard = OrderRegistrationBlockGuard(MockEnvironment().apply { setActiveProfiles("prod") })

        assertThatThrownBy { guard.assertNotBlocked() }
            .isInstanceOf(OrderRegistrationBlockedException::class.java)
    }

    @Test
    @DisplayName("dev 프로파일은 통과한다")
    fun passesOnDev() {
        val guard = OrderRegistrationBlockGuard(MockEnvironment().apply { setActiveProfiles("dev") })

        assertThatCode { guard.assertNotBlocked() }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("local 프로파일은 통과한다")
    fun passesOnLocal() {
        val guard = OrderRegistrationBlockGuard(MockEnvironment().apply { setActiveProfiles("local") })

        assertThatCode { guard.assertNotBlocked() }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("여러 프로파일 중 prod 가 포함되면 차단한다")
    fun blocksWhenProdAmongMultiple() {
        val guard = OrderRegistrationBlockGuard(MockEnvironment().apply { setActiveProfiles("dev", "prod") })

        assertThatThrownBy { guard.assertNotBlocked() }
            .isInstanceOf(OrderRegistrationBlockedException::class.java)
    }
}
