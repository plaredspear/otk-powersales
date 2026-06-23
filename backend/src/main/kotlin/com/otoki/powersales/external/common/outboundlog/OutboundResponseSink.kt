package com.otoki.powersales.external.common.outboundlog

import java.time.LocalDateTime

/**
 * [ExternalApiLogInterceptor] 가 호출별로 추가 적재를 위임하는 확장 포인트.
 *
 * 범용 인터셉터는 모든 외부 호출을 `external_api_log` 에 적재하지만, 특정 대상 시스템은 자기만의
 * 도메인 로그(예: SAP `sap_outbound_log` — interface_id / result_code / request_count) 를 추가로
 * 남겨야 한다. 그 도메인별 적재 규칙을 인터셉터 본문에서 분리하기 위한 콜백이다.
 *
 * - `common` 패키지가 도메인(`sap`) 을 역참조하지 않도록 함수형 인터페이스로만 선언하고, 구현체는
 *   각 도메인 패키지에 둔다 (의존 방향 sap → common 유지).
 * - sink 가 주입되면 인터셉터는 `captureBody` 와 무관하게 응답 본문을 1회 buffering 해 [accept] 로
 *   넘긴다 (예: SAP 는 prod 에서도 result_code 파싱을 위해 본문이 필요).
 * - [accept] 구현은 **예외를 던지지 않아야** 한다 (인터셉터가 best-effort 로 호출하지만, 자체적으로도
 *   격리할 것). 실제 외부 호출 결과에 영향을 주면 안 된다.
 */
fun interface OutboundResponseSink {

    /**
     * @param uri          호출 uri (query string 포함 가능)
     * @param requestBody  요청 본문 (bytes 디코드, captureBody 와 무관하게 항상 제공). 비어 있으면 빈 문자열.
     *                     count 산출 등 **가공 용도**이며, 그대로 저장하면 prod PII/용량 정책과 충돌할 수 있다
     * @param httpStatus   HTTP 상태 코드. 네트워크/연결 실패 시 null
     * @param responseBody 응답 본문. 네트워크 실패/캡처 실패 시 null
     * @param requestedAt  요청 시각
     * @param durationMs   소요시간(ms)
     * @param networkError 네트워크/IO 예외 등으로 응답을 받지 못했으면 true
     */
    fun accept(
        uri: String,
        requestBody: String,
        httpStatus: Int?,
        responseBody: String?,
        requestedAt: LocalDateTime,
        durationMs: Long,
        networkError: Boolean,
    )
}
