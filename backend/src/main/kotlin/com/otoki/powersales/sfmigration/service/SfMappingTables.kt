package com.otoki.powersales.sfmigration.service

/**
 * SF 데이터 마이그레이션 매핑 표 (한글 picklist → enum). 1회성 cut-over 용도라
 * sfmigration 도메인 내부에 상수로 보관 — 폐기 시 도메인 통째 삭제로 일관 청소.
 *
 * 매핑 source: scripts/sf-data-migration/common.kts (런칭 후 폐기).
 * SoT 정합 검증: UserRole enum 의 한글 라벨과 일치.
 *
 * Spec #806 이후 user.profile_type 컬럼 destructive 폐기로 ProfileType 매핑 표 제거.
 * Profile FK 산출은 spec #780 의 FK Resolve substep (Stage2FkService) 가 담당.
 */

internal val APP_AUTHORITY_TO_USER_ROLE: Map<String, String> = mapOf(
    "여사원" to "WOMAN",
    "조장" to "LEADER",
    "지점장" to "BRANCH_MANAGER",
    "영업부장" to "SALES_MANAGER",
    "사업부장" to "BUSINESS_MANAGER",
    "영업본부장" to "HEADQUARTERS_MANAGER",
    "영업지원실" to "SALES_SUPPORT",
    "시스템관리자" to "SYSTEM_ADMIN",
    "AccountViewAll" to "ACCOUNT_VIEW_ALL",
)
internal const val USER_ROLE_FALLBACK = "UNKNOWN"
