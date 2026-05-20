package com.otoki.powersales.user.entity

import org.slf4j.LoggerFactory

/**
 * 사용자 Profile 유형 enum (Spec #759).
 *
 * SF 레거시 Profile.Name 을 backend 도메인 enum 으로 매핑. SF 운영 raw 값
 * (`'5.영업사원'` / `'4.지점장'` 등 — 한글+숫자prefix, 공백 없음) 을 그대로
 * DB 저장 값으로 사용한다.
 *
 * - `value`: DB 저장 값 (VARCHAR(40)) — SF 운영 raw 값
 * - `sfProfileNames`: 동일 enum 으로 매핑되는 SF 표기 변형 (공백 포함 형태, normalized 형태, i18n 변형 등).
 *   `fromValue` 가 value 매칭 실패 시 본 목록을 fallback 으로 사용한다.
 *
 * 운영 환경에서 새 Profile 표기가 등장하면 `fromValue` 는 STAFF 로 fallback (WARN 로그).
 *
 * 산출 출처: SF `AppointmentTriggerHanlder.cls:313-365` 의 분기 + 운영 DB 실측 분포.
 */
enum class ProfileType(val value: String, val sfProfileNames: List<String>) {
    MARKETING("8.마케팅", listOf("8. 마케팅", "마케팅")),
    STAFF("9. Staff", listOf("9.Staff", "Staff")),
    TEAM_LEADER("6.조장", listOf("6. 조장", "조장")),
    BRANCH_MANAGER("4.지점장", listOf("4. 지점장", "지점장")),
    SALES_MANAGER("3.영업부장", listOf("3. 영업부장", "영업부장")),
    BUSINESS_DIRECTOR("2.사업부장", listOf("2. 사업부장", "사업부장")),
    DIVISION_HEAD("1.본부장", listOf("1. 본부장", "본부장")),
    SALES_REP("5.영업사원", listOf("5. 영업사원", "영업사원")),
    SALES_REP_LEADER("7.영업사원 + 조장", listOf("7. 영업사원 + 조장", "영업사원 + 조장", "영업사원+조장")),
    FACTORY_STAFF("공장관계자", emptyList()),
    OLS("OLS", emptyList()),
    SYSTEM_ADMIN("시스템 관리자", listOf("System Administrator", "システム管理者", "SYSTEM_ADMIN"));

    companion object {
        private val logger = LoggerFactory.getLogger(ProfileType::class.java)

        private val DB_VALUE_INDEX: Map<String, ProfileType> = entries.associateBy { it.value }

        private val SF_NAME_INDEX: Map<String, ProfileType> = entries
            .flatMap { type -> type.sfProfileNames.map { sfName -> sfName to type } }
            .toMap()

        /**
         * DB 저장 값(value) 으로 enum 역변환.
         *
         * 1차: `value` 직접 매칭.
         * 2차: `sfProfileNames` 의 표기 변형 매칭.
         * 3차 (미매칭): STAFF 디폴트 + WARN 로그 (SF AppointmentTriggerHandler 의 silently skip 동등).
         *
         * null/empty 입력만 null 을 반환 — 그 외 입력은 항상 ProfileType 반환을 보장한다.
         */
        fun fromValue(value: String?): ProfileType? {
            if (value.isNullOrEmpty()) return null
            DB_VALUE_INDEX[value]?.let { return it }
            SF_NAME_INDEX[value]?.let { return it }
            logger.warn("Unknown ProfileType DB value '{}' — fallback to STAFF", value)
            return STAFF
        }

        /**
         * 마이그레이션 helper — SF Profile.Name → backend enum 역매핑.
         *
         * SF 측의 한글+숫자prefix (`'9. Staff'`) 와 normalized 형태 (`'Staff'`) 둘 다 매칭한다.
         * 미매칭 시 STAFF 디폴트 반환 + WARN 로그 (SF AppointmentTriggerHandler 의 디폴트 동등).
         */
        fun fromSfProfileName(name: String?): ProfileType {
            if (name.isNullOrEmpty()) return STAFF
            DB_VALUE_INDEX[name]?.let { return it }
            SF_NAME_INDEX[name]?.let { return it }
            logger.warn("Unknown SF Profile.Name '{}' — fallback to STAFF", name)
            return STAFF
        }
    }
}
