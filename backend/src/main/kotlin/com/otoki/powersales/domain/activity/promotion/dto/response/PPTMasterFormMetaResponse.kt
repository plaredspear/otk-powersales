package com.otoki.powersales.domain.activity.promotion.dto.response

/**
 * 전문행사조 마스터 폼(등록/수정/복제 모달) 렌더링용 메타.
 *
 * 행사마스터 `PromotionFormMetaResponse` 와 동일한 "form 전용 API 분리" 패턴 —
 * 폼 Select 옵션을 프론트 상수로 하드코딩하지 않고 서버 enum 을 단일 출처로 내려준다.
 */
data class PPTMasterFormMetaResponse(
    val teamTypes: List<PPTTeamTypeOption>
)

/**
 * 전문행사조 유형 옵션.
 *
 * `teamType` 은 enum `@JsonValue` 가 한글 displayName 이라 저장/전송값이 곧 displayName —
 * 따라서 [value] 도 displayName 으로 내려준다(Select value 가 그대로 create/update payload 로 전송됨).
 */
data class PPTTeamTypeOption(
    val value: String,
    val name: String
)
