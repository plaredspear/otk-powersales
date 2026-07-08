package com.otoki.powersales.admin.tools.logging.service

import com.otoki.powersales.admin.tools.logging.dto.LoggerLevelResponse
import com.otoki.powersales.admin.tools.logging.dto.LoggerListResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LoggingSystem
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * 런타임 로그 레벨 조회/변경 서비스 (개발자 도구 > 대시보드).
 *
 * Spring Boot 의 [LoggingSystem] 을 직접 사용한다. Actuator `loggers` endpoint 를
 * 외부에 노출(`include: loggers`)하면 `/actuator/loggers` 가 Spring Security 체인 밖에
 * 있어(인증 없이 접근 가능) 로그 레벨을 아무나 조작할 수 있게 된다. 그래서 actuator 를
 * 열지 않고, 이 서비스를 `/api/v1/admin/` 하위 컨트롤러로 감싸 기존 web admin 인증 +
 * 시스템 관리자 가드를 그대로 적용한다.
 *
 * 변경은 메모리상 임시 조정으로 앱 재시작 시 application.yml 기본값으로 복귀한다.
 */
@Service
class LogLevelService(
    private val loggingSystem: LoggingSystem,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 관리 대상으로 노출할 로거 목록 — 운영 부하/PII 노출과 직결되는 핵심 로거만 선별한다.
     * 전체 로거(수천 개)를 나열하면 화면이 비대해지고 실수 여지가 커지므로, application.yml
     * `logging.level.*` 에 등장하는 카테고리를 그대로 노출한다. ROOT 는 항상 포함.
     */
    private val managedLoggerNames: List<String> = listOf(
        LoggingSystem.ROOT_LOGGER_NAME,
        "com.otoki.powersales",
        "org.springframework.security",
        "org.hibernate.SQL",
        "org.hibernate.type.descriptor.sql.BasicBinder",
    )

    /** 설정 가능한 레벨 목록 (LoggingSystem 이 지원하는 값, 문자열). */
    fun availableLevels(): List<String> =
        loggingSystem.supportedLogLevels.map { it.name }

    /** 관리 대상 로거 목록 + 선택 가능한 레벨 반환. */
    fun getLoggers(): LoggerListResponse {
        val loggers = managedLoggerNames.map { name ->
            val config = loggingSystem.getLoggerConfiguration(name)
            LoggerLevelResponse(
                name = name,
                configuredLevel = config?.configuredLevel?.name,
                effectiveLevel = config?.effectiveLevel?.name,
            )
        }
        return LoggerListResponse(
            availableLevels = availableLevels(),
            loggers = loggers,
        )
    }

    /**
     * 로거 레벨 변경. [rawLevel] 이 null/blank 이면 명시 레벨을 제거해 상속 상태로 되돌린다.
     *
     * @throws BusinessException 지원하지 않는 로거명 또는 레벨일 때 (400).
     */
    fun setLevel(loggerName: String, rawLevel: String?): LoggerLevelResponse {
        if (!managedLoggerNames.contains(loggerName)) {
            throw BusinessException(
                errorCode = "LOG_LEVEL_UNKNOWN_LOGGER",
                message = "관리 대상이 아닌 로거입니다: $loggerName",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        val level: LogLevel? = rawLevel?.takeIf { it.isNotBlank() }?.let { parseLevel(it) }
        loggingSystem.setLogLevel(loggerName, level)
        log.warn(
            "[log-level] 런타임 로그 레벨 변경 — logger={} level={}",
            loggerName,
            level?.name ?: "(상속 복귀)",
        )

        val config = loggingSystem.getLoggerConfiguration(loggerName)
        return LoggerLevelResponse(
            name = loggerName,
            configuredLevel = config?.configuredLevel?.name,
            effectiveLevel = config?.effectiveLevel?.name,
        )
    }

    private fun parseLevel(raw: String): LogLevel =
        try {
            LogLevel.valueOf(raw.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw BusinessException(
                errorCode = "LOG_LEVEL_INVALID",
                message = "지원하지 않는 로그 레벨입니다: $raw (허용: ${availableLevels().joinToString(", ")})",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
}
