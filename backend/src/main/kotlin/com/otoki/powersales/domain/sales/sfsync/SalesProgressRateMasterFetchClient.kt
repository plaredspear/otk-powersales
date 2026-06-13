package com.otoki.powersales.domain.sales.sfsync

/**
 * SF `SalesProgressRateMaster__c` fetch client (거래처목표등록마스터 주기 sync 입력원).
 *
 * SF 측 API 는 LastModifiedDate 기준으로 변경분을 응답할 예정이며, 본 client 는 그 응답을
 * [SalesProgressRateMasterFetchDto] 리스트로 반환한다. sync 서비스는 반환된 전체를 upsert 한다.
 *
 * endpoint / 인증 / SOQL / 페이지네이션 등 SF 통신 세부는 구현체([SalesProgressRateMasterFetchClientImpl])
 * 의 TODO 로 남겨둔다.
 */
interface SalesProgressRateMasterFetchClient {

    /**
     * SF 에서 거래처목표등록마스터 변경분을 가져온다.
     *
     * @return SF 응답 전체. 빈 리스트면 변경분 없음.
     */
    fun fetch(): List<SalesProgressRateMasterFetchDto>
}
