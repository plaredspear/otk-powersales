package com.otoki.powersales.external.sap.outbound.service

import com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.external.sap.outbound.repository.SapOutboundLogRepository
import com.otoki.powersales.external.sap.outbound.service.SapOutboundLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("SapOutboundLogService 테스트")
class SapOutboundLogServiceTest {

    private val repository: SapOutboundLogRepository = mockk()
    private val service = SapOutboundLogService(repository)

    @Test
    @DisplayName("log 호출 시 모든 필드를 매핑한 SapOutboundLog 엔티티가 저장된다")
    fun log_persistsEntityWithAllFields() {
        val captor = slot<SapOutboundLog>()
        every { repository.save(capture(captor)) } answers { firstArg() }

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

        val saved = captor.captured
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
