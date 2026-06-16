package com.otoki.powersales.domain.org.employee.sfsync

/**
 * SF `IF_SendStaffReviewToPWS` 사원평가 마스터 fetch client (주기 sync 입력원).
 *
 * SF 측 API 는 요청 body `MOD_DT`(기준 일자, 수정일 기준) 기준으로 변경분을 응답하며, 본 client 는 그 응답을
 * [StaffReviewFetchDto] 리스트로 반환한다. sync 서비스는 반환된 전체를 SF 레코드 Id(sfid) 기준 upsert 한다.
 *
 * endpoint / 인증 / 응답 파싱 등 SF 통신 세부는 구현체([StaffReviewFetchClientImpl]) 에 있다.
 */
interface StaffReviewFetchClient {

    /**
     * SF 에서 사원평가 마스터 변경분을 가져온다.
     *
     * @param modDt 조회 기준 일자 (YYYYMMDD). SF Request Body 의 `MOD_DT` 로 전송.
     * @return SF 응답 전체. 빈 리스트면 변경분 없음(또는 호출 실패 — 로그로 구분).
     */
    fun fetch(modDt: String): List<StaffReviewFetchDto>
}
