package com.otoki.powersales.external.sap.outbound.guard

/**
 * SAP REST 응답 본문에 HTML(`<` 문자) 이 포함되면 실패로 간주하는 일반 가드.
 *
 * 레거시 `IF_Util.cls:191-198` 의 정책 계승: HTTP 2xx 라도 응답 본문이 HTML 이면
 * SAP/proxy 측 에러 페이지로 보고 실패 처리한다 (Spec #588 §4.3).
 *
 * 출근 한정 로직 없음 — 모든 SAP outbound 컴포넌트에서 공통 적용 가능하다.
 */
object SapResponseHtmlGuard {

    /**
     * 응답 본문이 SAP 정상 JSON 인지 검증한다.
     * 본문이 null/blank 이면 검증 통과 (호출 측에서 별도 처리).
     */
    fun isValid(responseBody: String?): Boolean {
        if (responseBody.isNullOrBlank()) return true
        return !responseBody.contains('<')
    }
}
