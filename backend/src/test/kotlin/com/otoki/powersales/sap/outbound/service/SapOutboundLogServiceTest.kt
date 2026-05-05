package com.otoki.powersales.sap.outbound.service

import com.otoki.powersales.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.sap.outbound.repository.SapOutboundLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("SapOutboundLogService 테스트")
class SapOutboundLogServiceTest {

    @Mock
    private lateinit var repository: SapOutboundLogRepository

    @InjectMocks
    private lateinit var service: SapOutboundLogService

    @Test
    @DisplayName("log 호출 시 모든 필드를 매핑한 SapOutboundLog 엔티티가 저장된다")
    fun log_persistsEntityWithAllFields() {
        val captor = argumentCaptor<SapOutboundLog>()
        whenever(repository.save(any<SapOutboundLog>())).thenAnswer { it.getArgument<SapOutboundLog>(0) }

        val requestedAt = LocalDateTime.now().minusSeconds(3)
        val completedAt = LocalDateTime.now()
        service.log(
            interfaceId = "SD03300",
            endpointPath = "/api/sap/SD03300",
            requestCount = 5,
            httpStatus = 500,
            resultCode = "0",
            resultMsg = "FAIL",
            attemptCount = 3,
            durationMs = 1234L,
            errorDetail = "boom",
            requestedAt = requestedAt,
            completedAt = completedAt
        )

        org.mockito.kotlin.verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.interfaceId).isEqualTo("SD03300")
        assertThat(saved.endpointPath).isEqualTo("/api/sap/SD03300")
        assertThat(saved.requestCount).isEqualTo(5)
        assertThat(saved.httpStatus).isEqualTo(500)
        assertThat(saved.resultCode).isEqualTo("0")
        assertThat(saved.resultMsg).isEqualTo("FAIL")
        assertThat(saved.attemptCount).isEqualTo(3)
        assertThat(saved.durationMs).isEqualTo(1234L)
        assertThat(saved.errorDetail).isEqualTo("boom")
        assertThat(saved.requestedAt).isEqualTo(requestedAt)
        assertThat(saved.completedAt).isEqualTo(completedAt)
    }
}
