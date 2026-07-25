package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 진열사원 투입기준 마스터 폼(등록/수정 모달) 렌더링용 메타.
 *
 * 행사마스터 `/promotions/form-meta` 와 동일한 "form 전용 API 분리" 패턴 —
 * 폼 Select 옵션을 프론트 상수로 하드코딩하지 않고 서버를 단일 출처로 내려준다.
 *
 * - [accountCategories]: 구분(거래처유형마스터) 옵션. 기존 `/account-categories` lookup 을 흡수한다.
 * - [typeOfWork1Options]: 근무형태1 옵션. 서버 enum([com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1])
 *   이 단일 출처이며, 기존에는 프론트 상수로 하드코딩되어 있었다.
 */
data class EmployeeInputCriteriaMasterFormMetaResponse(
    val accountCategories: List<AccountCategoryOption>,
    val typeOfWork1Options: List<TypeOfWork1Option>,
)

/** 구분(거래처유형마스터) 옵션. [value] 는 저장 시 전송하는 categoryId. */
data class AccountCategoryOption(
    val value: Long,
    val accountCode: String,
    val name: String,
)

/** 근무형태1 옵션. [value] 는 API 가 주고받는 표시명(enum 의 `@JsonValue`)과 동일하다. */
data class TypeOfWork1Option(
    val value: String,
    val name: String,
)
