package com.otoki.powersales.user.entity

import org.slf4j.LoggerFactory

/**
 * 사용자 Profile 유형 enum (Spec #759).
 *
 * SF 레거시 Profile.Name 의 backend 매핑. SF Profile.Name 의 한글+숫자prefix
 * (`'4. 지점장'` / `'9. Staff'`) 는 모두 영문 enum constant 로 통합한다.
 * i18n 정합: `시스템 관리자` / `System Administrator` / `システム管理者` 3개는
 * 모두 `SYSTEM_ADMIN` 단일 상수로 매핑된다.
 *
 * - `value`: DB 저장 값 (VARCHAR(40)) — backend enum constant 이름과 동일
 * - `sfProfileNames`: SF Profile.Name 매핑 (마이그레이션 시 SF.Profile.Name → enum 역매핑 시 사용)
 *
 * 산출 출처: SF `AppointmentTriggerHanlder.cls:313-365` 의 10개 분기.
 */
enum class ProfileType(val value: String, val sfProfileNames: List<String>) {
    MARKETING("MARKETING", listOf("8. 마케팅", "마케팅")),
    STAFF("STAFF", listOf("9. Staff", "Staff")),
    TEAM_LEADER("TEAM_LEADER", listOf("6. 조장", "조장")),
    BRANCH_MANAGER("BRANCH_MANAGER", listOf("4. 지점장", "지점장")),
    SALES_MANAGER("SALES_MANAGER", listOf("영업부장")),
    BUSINESS_DIRECTOR("BUSINESS_DIRECTOR", listOf("사업부장")),
    DIVISION_HEAD("DIVISION_HEAD", listOf("본부장")),
    SALES_REP("SALES_REP", listOf("5. 영업사원", "영업사원")),
    SYSTEM_ADMIN("SYSTEM_ADMIN", listOf("시스템 관리자", "System Administrator", "システム管理者"));

    companion object {
        private val logger = LoggerFactory.getLogger(ProfileType::class.java)

        private val DB_VALUE_INDEX: Map<String, ProfileType> = entries.associateBy { it.value }

        private val SF_NAME_INDEX: Map<String, ProfileType> = entries
            .flatMap { type -> type.sfProfileNames.map { sfName -> sfName to type } }
            .toMap()

        /**
         * DB 저장 값(value) 으로 enum 역변환. 미인지 값은 null.
         */
        fun fromValue(value: String?): ProfileType? {
            if (value.isNullOrEmpty()) return null
            return DB_VALUE_INDEX[value]
        }

        /**
         * 마이그레이션 helper — SF Profile.Name → backend enum 역매핑.
         *
         * SF 측의 한글+숫자prefix (`'9. Staff'`) 와 normalized 형태 (`'Staff'`) 둘 다 매칭한다.
         * 미매칭 시 STAFF 디폴트 반환 + WARN 로그 (SF AppointmentTriggerHandler 의 디폴트 동등).
         */
        fun fromSfProfileName(name: String?): ProfileType {
            if (name.isNullOrEmpty()) return STAFF
            val matched = SF_NAME_INDEX[name]
            if (matched != null) return matched
            logger.warn("Unknown SF Profile.Name '{}' — fallback to STAFF", name)
            return STAFF
        }
    }
}
