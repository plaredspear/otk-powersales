package com.otoki.powersales.admin.tools.logging

import com.otoki.powersales.admin.tools.logging.service.LogLevelService
import com.otoki.powersales.platform.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggerConfiguration
import org.springframework.boot.logging.LoggingSystem

@DisplayName("LogLevelService 테스트")
class LogLevelServiceTest {

    private val loggingSystem: LoggingSystem = mockk(relaxed = true)
    private val service = LogLevelService(loggingSystem)

    private fun config(name: String, configured: LogLevel?, effective: LogLevel): LoggerConfiguration =
        LoggerConfiguration(name, configured, effective)

    @Test
    @DisplayName("getLoggers - 관리 대상 로거 목록 + 지원 레벨 반환")
    fun getLoggers_returnsManagedLoggers() {
        every { loggingSystem.supportedLogLevels } returns setOf(
            LogLevel.OFF, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE,
        )
        every { loggingSystem.getLoggerConfiguration(any()) } answers {
            config(firstArg(), null, LogLevel.INFO)
        }
        every { loggingSystem.getLoggerConfiguration("org.hibernate.SQL") } returns
            config("org.hibernate.SQL", LogLevel.DEBUG, LogLevel.DEBUG)

        val result = service.getLoggers()

        assertThat(result.availableLevels).contains("INFO", "DEBUG", "OFF")
        // ROOT + com.otoki.powersales + security + hibernate.SQL + BasicBinder = 5개
        assertThat(result.loggers).hasSize(5)
        assertThat(result.loggers.map { it.name })
            .containsExactly(
                "ROOT",
                "com.otoki.powersales",
                "org.springframework.security",
                "org.hibernate.SQL",
                "org.hibernate.type.descriptor.sql.BasicBinder",
            )
        val sqlLogger = result.loggers.first { it.name == "org.hibernate.SQL" }
        assertThat(sqlLogger.configuredLevel).isEqualTo("DEBUG")
    }

    @Test
    @DisplayName("setLevel - 유효 레벨이면 LoggingSystem.setLogLevel 호출")
    fun setLevel_appliesLevel() {
        val levelSlot = slot<LogLevel>()
        every { loggingSystem.setLogLevel("com.otoki.powersales", capture(levelSlot)) } returns Unit
        every { loggingSystem.getLoggerConfiguration("com.otoki.powersales") } returns
            config("com.otoki.powersales", LogLevel.DEBUG, LogLevel.DEBUG)

        val result = service.setLevel("com.otoki.powersales", "debug")

        assertThat(levelSlot.captured).isEqualTo(LogLevel.DEBUG)
        assertThat(result.configuredLevel).isEqualTo("DEBUG")
        verify(exactly = 1) { loggingSystem.setLogLevel("com.otoki.powersales", LogLevel.DEBUG) }
    }

    @Test
    @DisplayName("setLevel - level null 이면 상속 복귀(null 로 setLogLevel)")
    fun setLevel_nullResetsToInherited() {
        every { loggingSystem.setLogLevel("com.otoki.powersales", null) } returns Unit
        every { loggingSystem.getLoggerConfiguration("com.otoki.powersales") } returns
            config("com.otoki.powersales", null, LogLevel.INFO)

        val result = service.setLevel("com.otoki.powersales", null)

        assertThat(result.configuredLevel).isNull()
        verify(exactly = 1) { loggingSystem.setLogLevel("com.otoki.powersales", null) }
    }

    @Test
    @DisplayName("setLevel - 관리 대상 아닌 로거는 400 BusinessException")
    fun setLevel_unknownLoggerRejected() {
        assertThatThrownBy { service.setLevel("com.evil.arbitrary", "DEBUG") }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("관리 대상이 아닌 로거")

        verify(exactly = 0) { loggingSystem.setLogLevel(any(), any()) }
    }

    @Test
    @DisplayName("setLevel - 지원하지 않는 레벨은 400 BusinessException")
    fun setLevel_invalidLevelRejected() {
        every { loggingSystem.supportedLogLevels } returns setOf(LogLevel.INFO, LogLevel.DEBUG)

        assertThatThrownBy { service.setLevel("com.otoki.powersales", "VERBOSE") }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("지원하지 않는 로그 레벨")

        verify(exactly = 0) { loggingSystem.setLogLevel(any(), any()) }
    }
}
