package com.otoki.powersales.domain.org.employee.dto.response

/**
 * 여사원 상세 폼(수정 모달) 렌더링용 메타 — "form 전용 API 분리" 표준 패턴.
 *
 * 행사마스터 `/promotions/form-meta` / 전문행사조 마스터 `/ppt-masters/form-meta` 와 동일하게,
 * 폼 Select 옵션을 프론트 상수로 하드코딩하지 않고 서버를 단일 출처로 삼는다.
 *
 * ## 목록 `/meta` 와의 구분
 *
 * 같은 도메인의 [FemaleEmployeeListMetaResponse] 는 **목록 화면 검색 필터** 용이라 구조
 * (`filters` / `defaults`) 와 내용이 모두 다르다. 특히 전문행사조는 필터에 검색 전용 선택지
 * ('행사조 전체') 가 포함되지만 폼에는 저장 가능한 값만 와야 하므로, 둘을 재사용하지 않고 분리한다.
 */
data class FemaleEmployeeFormMetaResponse(
    /** 재직상태 — 재직 / 휴직 / 퇴직. */
    val statuses: List<FemaleEmployeeFormOption>,
    /** 권한 — SF `DKRetail__AppAuthority__c` picklist 4종. */
    val roles: List<FemaleEmployeeFormOption>,
    /** 전문행사조 — '일반'(미배정) + 정식 5개 조. */
    val professionalPromotionTeams: List<FemaleEmployeeFormOption>,
)

/**
 * 폼 Select 옵션 1개.
 *
 * [value] 는 수정 요청에 그대로 실려 나가는 저장/전송 값이고, [label] 은 화면 표기용이다.
 * 대부분 동일하나 권한의 `AccountViewAll` 처럼 raw value 만으로 의미가 드러나지 않는 값은
 * label 에 한글을 병기한다.
 */
data class FemaleEmployeeFormOption(
    val value: String,
    val label: String,
)
