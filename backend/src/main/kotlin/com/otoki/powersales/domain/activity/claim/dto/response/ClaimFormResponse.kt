package com.otoki.powersales.domain.activity.claim.dto.response

/**
 * 클레임 등록 화면 진입 폼 조회 응답 DTO.
 *
 * 화면 진입 시 한 번의 호출로 폼 렌더링에 필요한 정적 메타데이터([metadata])와
 * 이어쓰기용 임시저장([draft], 없으면 null)을 함께 내려준다.
 * 일매출 마감 폼([com.otoki.powersales.promotion.dto.response.DailySalesFormResponse]) 의
 * "진입 1콜 + draft prefill 동봉" 컨벤션과 정합한다.
 *
 * 레거시(otg_PowerSales)는 SSR 진입 시점에 임시저장 유무를 즉시 확인해 "이어서 작성?"
 * 팝업을 띄웠다. 신규는 SPA/네이티브이므로 이 진입 조회로 동일 흐름을 구성한다.
 */
data class ClaimFormResponse(
    /** 클레임 종류1/2·구매방법·요청사항 등 정적 picklist. */
    val metadata: ClaimFormDataResponse,
    /** 사원의 임시저장(없으면 null). 있으면 화면이 "이어서 작성?" 후 prefill 한다. */
    val draft: ClaimDraftResponse?
)
