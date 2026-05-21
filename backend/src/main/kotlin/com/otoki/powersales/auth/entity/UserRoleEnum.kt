package com.otoki.powersales.auth.entity

import org.slf4j.LoggerFactory

/**
 * 사용자 권한 Enum.
 *
 * Salesforce `DKRetail__Employee__c.DKRetail__AppAuthority__c` picklist 의 4 옵션값과 SAP 직위명을
 * 함께 표현. `korean` 필드는 SF 원본 옵션값을 그대로 보존 (한글 또는 영문, e.g. `AccountViewAll`).
 * `UNKNOWN` 은 SF/SAP 동기화 시 매핑 실패한 값 또는 DB 의 미정의 값에 대한 fallback 이며,
 * 어떤 운영 그룹 상수에도 포함되지 않아 모든 권한 판정에서 false 가 된다.
 */
enum class UserRoleEnum(val korean: String) {
    WOMAN("여사원"),
    LEADER("조장"),
    BRANCH_MANAGER("지점장"),
    SALES_MANAGER("영업부장"),
    BUSINESS_MANAGER("사업부장"),
    HEADQUARTERS_MANAGER("영업본부장"),
    SALES_SUPPORT("영업지원실"),
    SYSTEM_ADMIN("시스템관리자"),
    ACCOUNT_VIEW_ALL("AccountViewAll"),
    UNKNOWN("(미인지)");

    fun toKorean(): String = korean

    companion object {
        private val logger = LoggerFactory.getLogger(UserRoleEnum::class.java)

        /** 전 지점 데이터 조회 가능 */
        val ALL_BRANCHES: Set<UserRoleEnum> = setOf(
            SALES_MANAGER, BUSINESS_MANAGER, HEADQUARTERS_MANAGER, SALES_SUPPORT
        )

        /** 자기 지점만 조회 */
        val BRANCH_SCOPE: Set<UserRoleEnum> = setOf(LEADER, BRANCH_MANAGER)

        /** 여사원 한정 */
        val WOMAN_ONLY: Set<UserRoleEnum> = setOf(WOMAN)

        /** 관리자급 (일정 무제한 변경 등) */
        val ADMIN_GRADE: Set<UserRoleEnum> = setOf(SYSTEM_ADMIN, SALES_SUPPORT)

        /** Web Admin 로그인 허용 (8개 운영 역할 중 WOMAN 제외 7개) */
        val ALLOWED_FOR_ADMIN_LOGIN: Set<UserRoleEnum> = setOf(
            LEADER, BRANCH_MANAGER, SALES_MANAGER, BUSINESS_MANAGER,
            HEADQUARTERS_MANAGER, SALES_SUPPORT, SYSTEM_ADMIN
        )

        /** 권한 매트릭스 변경 가능 */
        val MANAGE_PERMISSIONS: Set<UserRoleEnum> = setOf(SYSTEM_ADMIN)

        private val KOREAN_TO_OPERATIONAL_ROLE: Map<String, UserRoleEnum> = entries
            .filter { it != UNKNOWN }
            .associateBy { it.korean }

        /**
         * 한글 표시명을 enum 으로 변환한다.
         *
         * - `null` 또는 빈 문자열 → `null`
         * - 운영 역할 8종 한글 매칭 → 해당 enum
         * - 매칭 실패 → WARN 로그 후 `UNKNOWN`
         */
        fun fromKorean(value: String?): UserRoleEnum? {
            if (value.isNullOrEmpty()) return null
            val matched = KOREAN_TO_OPERATIONAL_ROLE[value]
            if (matched != null) return matched
            logger.warn("Unknown UserRole korean value: {}", value)
            return UNKNOWN
        }
    }
}
