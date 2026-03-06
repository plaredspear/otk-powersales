package com.otoki.internal.sap.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.dto.SapSyncError
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.sap.entity.SapSyncLog
import com.otoki.internal.sap.repository.SapSyncLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
@DisplayName("SapSyncLogService 테스트")
class SapSyncLogServiceTest {

    @Mock
    private lateinit var sapSyncLogRepository: SapSyncLogRepository

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @InjectMocks
    private lateinit var sapSyncLogService: SapSyncLogService

    @Nested
    @DisplayName("log - 동기화 로그 기록")
    inner class LogTests {

        @Test
        @DisplayName("성공 결과 기록 - 에러 없음")
        fun logSuccess_noErrors() {
            val result = SapSyncResult(successCount = 10, failCount = 0)
            val captor = argumentCaptor<SapSyncLog>()
            whenever(sapSyncLogRepository.save(captor.capture())).thenAnswer { it.getArgument<SapSyncLog>(0) }

            sapSyncLogService.log(
                apiName = "organize-master",
                requestCount = 10,
                result = result,
                durationMs = 500,
                requestIp = "10.0.1.100",
                requestedAt = LocalDateTime.now()
            )

            val saved = captor.firstValue
            assertThat(saved.apiName).isEqualTo("organize-master")
            assertThat(saved.requestCount).isEqualTo(10)
            assertThat(saved.successCount).isEqualTo(10)
            assertThat(saved.failCount).isEqualTo(0)
            assertThat(saved.errorDetail).isNull()
            assertThat(saved.durationMs).isEqualTo(500)
            assertThat(saved.requestIp).isEqualTo("10.0.1.100")
        }

        @Test
        @DisplayName("부분 실패 결과 기록 - 에러 상세 JSON 포함")
        fun logPartialFailure_withErrors() {
            val errors = listOf(
                SapSyncError(index = 3, field = "emp_code", value = "999999", error = "사원코드 형식 오류")
            )
            val result = SapSyncResult(successCount = 9, failCount = 1, errors = errors)
            val errorJson = """[{"index":3,"field":"emp_code","value":"999999","error":"사원코드 형식 오류"}]"""
            whenever(objectMapper.writeValueAsString(errors)).thenReturn(errorJson)

            val captor = argumentCaptor<SapSyncLog>()
            whenever(sapSyncLogRepository.save(captor.capture())).thenAnswer { it.getArgument<SapSyncLog>(0) }

            sapSyncLogService.log(
                apiName = "employee-master",
                requestCount = 10,
                result = result,
                durationMs = 1200,
                requestIp = "10.0.1.100",
                requestedAt = LocalDateTime.now()
            )

            val saved = captor.firstValue
            assertThat(saved.failCount).isEqualTo(1)
            assertThat(saved.errorDetail).isEqualTo(errorJson)
        }
    }
}
