package com.otoki.powersales.account.policy

/**
 * 신규 거래처 등록 시 거래처명에 contains 가능한 prefix 화이트리스트. (Spec #640)
 *
 * 레거시 SF Custom Label `AccountPrefix` (예: `신규/기타`) 의 인계 — application Constants 로 이전.
 * 운영 변경(prefix 추가/삭제) 발생 시 본 [ALLOWED] 1곳만 갱신한다 (외부 설정 분리 미도입 — 변경 빈도 낮음).
 *
 * 레거시 검증 동작 (`AccountTriggerHandler.cls:73-82`): split 결과 `['신규','기타']` 중 하나가
 * `'(' + p + ')'` 형태로 name 에 포함되어야 함. 신규 시스템은 이미 parens 포함 형태(`(신규)`, `(기타)`)
 * 로 ALLOWED 에 보관하여 contains 1회 비교로 동등 의미 보존.
 */
object AccountNamePrefix {

    val ALLOWED: List<String> = listOf("(신규)", "(기타)")

    /**
     * [name] 이 [ALLOWED] 중 하나라도 contains 하면 `true`. (입력은 trim 된 상태 가정)
     */
    fun isValidName(name: String): Boolean = ALLOWED.any { prefix -> name.contains(prefix) }

    /**
     * 사용자 표시용 prefix 목록 — `/` 로 join. 예: `(신규)/(기타)`.
     * 검증 실패 에러 메시지의 `[prefix list]` 자리에 삽입한다.
     */
    fun joinForMessage(): String = ALLOWED.joinToString("/")
}
