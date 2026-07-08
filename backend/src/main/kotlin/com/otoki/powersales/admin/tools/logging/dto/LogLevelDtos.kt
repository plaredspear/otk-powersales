package com.otoki.powersales.admin.tools.logging.dto

import jakarta.validation.constraints.NotBlank

/**
 * 로그 레벨 관리 화면 (개발자 도구 > 대시보드) 요청/응답 DTO.
 *
 * Spring `LoggingSystem` 을 통해 런타임 로그 레벨을 조회/변경하는 데 쓰인다.
 * 변경은 메모리상 임시 조정으로, 앱 재시작 시 application.yml 기본값으로 복귀한다
 * (지속 저장 없음 — 개발자 도구 특성상 임시 상향/복원 용도).
 */

/** 개별 로거의 레벨 상태. */
data class LoggerLevelResponse(
    /** 로거 이름 (패키지/클래스 FQCN). ROOT 는 `"ROOT"`. */
    val name: String,
    /**
     * 명시적으로 설정된 레벨. 상위 로거로부터 상속만 받는 경우 null.
     * 값 예: OFF / ERROR / WARN / INFO / DEBUG / TRACE.
     */
    val configuredLevel: String?,
    /** 실제 적용되는 유효 레벨 (상속 해소 후). */
    val effectiveLevel: String?,
)

/** 로거 목록 + 선택 가능한 레벨 집합. */
data class LoggerListResponse(
    /** 설정 가능한 레벨 목록 (LoggingSystem 이 지원하는 값). */
    val availableLevels: List<String>,
    /** 관리 대상 로거 목록. */
    val loggers: List<LoggerLevelResponse>,
)

/**
 * 로그 레벨 변경 요청.
 *
 * - `loggerName` 은 body 로 받는다. `org.hibernate.SQL` 처럼 점(.)이 포함된 로거명을 URL
 *   path segment 로 두면 마지막 `.SQL` 이 확장자로 절단되거나 매칭이 까다로워 body 로 받는다.
 * - `level` 이 null 이면 명시 레벨을 제거해 상위 로거로부터 상속 상태로 되돌린다
 *   (Actuator loggers 의 configuredLevel=null 과 동일 의미).
 */
data class UpdateLoggerLevelRequest(
    @field:NotBlank(message = "로거명은 필수입니다")
    val loggerName: String,
    val level: String?,
)
