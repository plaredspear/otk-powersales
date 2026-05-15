package com.otoki.powersales.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 단말 UUID 검증 정책 (Spec #582 P1-B §3.4).
 *
 * 레거시 `otokipowersales.uuid.check` / `otokipowersales.uuid.excluded.ids` 환경변수의 신규 표기.
 *
 * - `enabled=false` 인 경우 단말 바인딩 검증 자체를 건너뛴다 (기본 true).
 * - `excludedEmployeeCodes` 에 포함된 사번은 단말 바인딩 검증을 면제한다 (CSV).
 */
@ConfigurationProperties(prefix = "app.auth.uuid-check")
data class UuidCheckProperties(
    val enabled: Boolean = true,
    val excludedEmployeeCodes: String = ""
) {
    fun isExcluded(employeeCode: String): Boolean {
        if (excludedEmployeeCodes.isBlank()) return false
        return excludedEmployeeCodes.split(",").map { it.trim() }.contains(employeeCode)
    }
}
