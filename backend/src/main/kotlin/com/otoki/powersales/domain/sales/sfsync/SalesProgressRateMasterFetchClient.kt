package com.otoki.powersales.domain.sales.sfsync

/**
 * SF `IF_salesprogresssend` 거래처목표등록마스터 fetch client (주기 sync 입력원).
 *
 * SF 측 API 는 요청 body `MOD_DT`(기준 일자) 기준으로 변경분을 응답하며, 본 client 는 그 응답을
 * [SalesProgressRateMasterFetchDto] 리스트로 반환한다. sync 서비스는 반환된 전체를 ExternalKey 기준 upsert 한다.
 *
 * endpoint / 인증 / 응답 파싱 등 SF 통신 세부는 구현체([SalesProgressRateMasterFetchClientImpl]) 에 있다.
 */
interface SalesProgressRateMasterFetchClient {

    /**
     * SF 에서 거래처목표등록마스터 변경분을 가져온다.
     *
     * @param modDt 조회 기준 일자 (YYYYMMDD). SF Request Body 의 `MOD_DT` 로 전송.
     * @return SF 응답 전체. 빈 리스트면 변경분 없음(또는 호출 실패 — 로그로 구분).
     */
    fun fetch(modDt: String): List<SalesProgressRateMasterFetchDto>

    /**
     * 이미 확보한 SF 응답 rawBody(JSON) 를 [SalesProgressRateMasterFetchDto] 리스트로 변환한다.
     *
     * SF 를 재호출하지 않고 응답 원형만 재해석하는 경로 (개발자 도구의 "조회 + 저장" 이 raw 노출과
     * 저장에 같은 응답 1회분을 공유할 때 사용). 형식 불명/파싱 실패 시 빈 리스트.
     */
    fun parse(raw: String?): List<SalesProgressRateMasterFetchDto>
}
