package com.otoki.powersales.common.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.health.contributor.Status

// HealthDescriptor 는 sealed (PermittedSubclasses: Indicated/Composite). 두 서브타입 모두 생성자 package-private 이라
// 직접 인스턴스화 불가하지만 concrete class 라 Mockito.mock 으로 stub 가능.
@DisplayName("HealthController 테스트")
class HealthControllerTest {

    private val healthEndpoint: HealthEndpoint = mock(HealthEndpoint::class.java)
    private val controller = HealthController(healthEndpoint)

    private fun stubIndicator(status: Status): IndicatedHealthDescriptor {
        val d = mock(IndicatedHealthDescriptor::class.java)
        whenever(d.status).thenReturn(status)
        return d
    }

    @Nested
    @DisplayName("GET /api/health - 헬스 응답")
    inner class GetHealth {

        @Test
        @DisplayName("CompositeHealthDescriptor + db/redis 모두 UP - 모든 컴포넌트 status UP")
        fun composite_dbAndRedisUp() {
            val db = stubIndicator(Status.UP)
            val redis = stubIndicator(Status.UP)
            val composite: CompositeHealthDescriptor = mock(CompositeHealthDescriptor::class.java)
            whenever(composite.status).thenReturn(Status.UP)
            whenever(composite.components).thenReturn(mapOf("db" to db, "redis" to redis))
            whenever(healthEndpoint.health()).thenReturn(composite)

            val response = controller.health()

            assertThat(response.status).isEqualTo("UP")
            assertThat(response.components.db?.status).isEqualTo("UP")
            assertThat(response.components.redis?.status).isEqualTo("UP")
        }

        @Test
        @DisplayName("CompositeHealthDescriptor + redis DOWN - 루트 status DOWN, redis 컴포넌트 DOWN")
        fun composite_redisDown() {
            val db = stubIndicator(Status.UP)
            val redis = stubIndicator(Status.DOWN)
            val composite: CompositeHealthDescriptor = mock(CompositeHealthDescriptor::class.java)
            whenever(composite.status).thenReturn(Status.DOWN)
            whenever(composite.components).thenReturn(mapOf("db" to db, "redis" to redis))
            whenever(healthEndpoint.health()).thenReturn(composite)

            val response = controller.health()

            assertThat(response.status).isEqualTo("DOWN")
            assertThat(response.components.db?.status).isEqualTo("UP")
            assertThat(response.components.redis?.status).isEqualTo("DOWN")
        }

        @Test
        @DisplayName("CompositeHealthDescriptor 의 components 에 db/redis 부재 - 해당 필드 null, 500 아님")
        fun composite_missingComponents() {
            val composite: CompositeHealthDescriptor = mock(CompositeHealthDescriptor::class.java)
            whenever(composite.status).thenReturn(Status.UP)
            whenever(composite.components).thenReturn(emptyMap())
            whenever(healthEndpoint.health()).thenReturn(composite)

            val response = controller.health()

            assertThat(response.status).isEqualTo("UP")
            assertThat(response.components.db).isNull()
            assertThat(response.components.redis).isNull()
        }

        @Test
        @DisplayName("HealthDescriptor 가 Composite 가 아닌 경우 - components 모두 null, status 는 루트 유지")
        fun nonComposite_allComponentsNull() {
            val indicated = mock(IndicatedHealthDescriptor::class.java)
            whenever(indicated.status).thenReturn(Status.UNKNOWN)
            whenever(healthEndpoint.health()).thenReturn(indicated)

            val response = controller.health()

            assertThat(response.status).isEqualTo("UNKNOWN")
            assertThat(response.components.db).isNull()
            assertThat(response.components.redis).isNull()
        }
    }
}
