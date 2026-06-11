package com.otoki.powersales.admin.dto.response

/**
 * 외부 API 연동 정보 응답 (개발자 도구 — 외부 API 테스트).
 *
 * 각 외부 API 탭이 "요청 시 필수 연동 정보"(외부 시스템 endpoint / HTTP method / 인증 방식)를 노출하도록,
 * backend 가 현재 환경에 주입된 실제 설정값을 조회해 내려준다. 인증 secret(비밀번호/client secret)은
 * 절대 본문에 포함하지 않고 [authType] 으로 방식만 표기한다.
 */
data class ExternalApiIntegrationInfoResponse(
    val items: List<ExternalApiIntegrationInfo>,
)

/**
 * 단일 외부 API 의 연동 정보.
 *
 * @property key            web 탭 key (예: `claim-regist`, `loan-inquiry`, `naver-geocode`) — 탭 매칭용.
 * @property externalSystem 호출 대상 외부 시스템 (예: `Salesforce`, `SAP`, `Naver Cloud Platform`).
 * @property endpoint       backend 가 실제 호출하는 외부 endpoint 전체 URL. baseUrl 미설정 시 `(미설정)` 표기.
 * @property httpMethod     외부 호출 HTTP method (`GET` / `POST`).
 * @property authType       인증 방식 설명 (secret 값 없이 방식만). 예: `OAuth2 Password Grant (Bearer)`.
 * @property note           추가 연동 메모 (인터페이스 ID, 환경변수 prefix 등).
 */
data class ExternalApiIntegrationInfo(
    val key: String,
    val externalSystem: String,
    val endpoint: String,
    val httpMethod: String,
    val authType: String,
    val note: String,
)
