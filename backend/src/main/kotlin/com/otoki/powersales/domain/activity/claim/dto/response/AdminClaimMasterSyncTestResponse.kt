package com.otoki.powersales.domain.activity.claim.dto.response

/**
 * SF `IF_SendClaimToPWS` 클레임 마스터 조회 + 갱신 응답 (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향으로 변경 클레임 마스터를 조회한 뒤, 각 레코드의 `pwrskey`(=claim_id) 로 신규 claim 을
 * 찾아 조치/상담 필드(조치상태/조치코드/상담번호/사유유형/조치내용/COSMOS)를 갱신한 결과를 함께 담는다.
 * 갱신 대상 필드는 SF 레거시 inbound Apex SET 절 정합(6개)이다.
 *
 * @property success      SF 호출 성공 여부 (HTTP 200 + RESULT_CODE 정상).
 * @property resultCode   SF 응답의 `RESULT_CODE` (응답에 없으면 null).
 * @property resultMsg    SF 응답의 `RESULT_MSG` 또는 오류 요약 (응답에 없으면 null).
 * @property rawResponse  SF 응답 본문(raw JSON). 클레임 마스터 목록이 이 안에 담겨 온다.
 * @property requestPayload SF 로 전송한 요청 body JSON (`{ "MOD_DT": "..." }`).
 * @property fetchedCount 응답에서 파싱한 클레임 레코드 수.
 * @property updatedCount pwrskey 매칭에 성공해 갱신한 claim 수.
 * @property notFoundCount pwrskey 가 가리키는 claim 이 신규 DB 에 없어 건너뛴 수.
 * @property skippedCount pwrskey 가 비어있거나 숫자가 아니어서 매칭 불가로 건너뛴 수.
 */
data class AdminClaimMasterSyncTestResponse(
    val success: Boolean,
    val resultCode: String?,
    val resultMsg: String?,
    val rawResponse: String?,
    val requestPayload: String,
    val fetchedCount: Int = 0,
    val updatedCount: Int = 0,
    val notFoundCount: Int = 0,
    val skippedCount: Int = 0,
)
