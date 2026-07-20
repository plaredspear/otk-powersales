package com.otoki.powersales.platform.auth.policy

/**
 * 임시 비밀번호 발급 규칙.
 *
 * 관리자/조장의 비밀번호 초기화 및 마이그레이션 초기 비밀번호 발급 시 사용하는 평문을 산출한다.
 * 사번 기반 `"{사번}@pwrs"` 를 기본으로 하며, 사번이 없는 계정(사원 미매칭 순수 관리자 계정) 은
 * 종전 고정값 [FALLBACK_PASSWORD] 로 되돌아간다.
 *
 * 발급된 평문은 응답에 담지 않는다 — 화면이 대상자의 사번으로 동일 규칙을 재조립해 안내한다.
 * 초기화 직후에는 항상 `passwordChangeRequired = true` 가 함께 설정되어 최초 로그인 시 변경이 강제된다.
 */
object TemporaryPasswordPolicy {

    const val SUFFIX = "@pwrs"

    /** 사번이 없는 계정용 종전 고정 임시 비밀번호. */
    const val FALLBACK_PASSWORD = "pwrs1234!"

    /**
     * [employeeCode] 사원의 임시 비밀번호 평문을 산출한다.
     *
     * 사번이 null 이거나 공백이면 [FALLBACK_PASSWORD] 를 반환한다.
     */
    fun forEmployeeCode(employeeCode: String?): String =
        employeeCode?.takeIf { it.isNotBlank() }?.let { "$it$SUFFIX" } ?: FALLBACK_PASSWORD
}
