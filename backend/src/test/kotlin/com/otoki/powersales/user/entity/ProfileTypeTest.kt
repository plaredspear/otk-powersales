package com.otoki.powersales.user.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProfileType — DB value 역매핑 + SF Profile.Name 마이그레이션 helper")
class ProfileTypeTest {

    @Test
    @DisplayName("fromValue — DB 저장 값 → enum")
    fun fromValueMatching() {
        assertThat(ProfileType.fromValue("MARKETING")).isEqualTo(ProfileType.MARKETING)
        assertThat(ProfileType.fromValue("STAFF")).isEqualTo(ProfileType.STAFF)
        assertThat(ProfileType.fromValue("SYSTEM_ADMIN")).isEqualTo(ProfileType.SYSTEM_ADMIN)
    }

    @Test
    @DisplayName("fromValue — 미인지 값 → null")
    fun fromValueUnknown() {
        assertThat(ProfileType.fromValue("UNKNOWN_TYPE")).isNull()
        assertThat(ProfileType.fromValue(null)).isNull()
        assertThat(ProfileType.fromValue("")).isNull()
    }

    @Test
    @DisplayName("fromSfProfileName — SF Profile.Name 한글+숫자prefix 형태 매칭 (9개 상수)")
    fun fromSfProfileNameWithPrefix() {
        assertThat(ProfileType.fromSfProfileName("8. 마케팅")).isEqualTo(ProfileType.MARKETING)
        assertThat(ProfileType.fromSfProfileName("9. Staff")).isEqualTo(ProfileType.STAFF)
        assertThat(ProfileType.fromSfProfileName("6. 조장")).isEqualTo(ProfileType.TEAM_LEADER)
        assertThat(ProfileType.fromSfProfileName("4. 지점장")).isEqualTo(ProfileType.BRANCH_MANAGER)
        assertThat(ProfileType.fromSfProfileName("영업부장")).isEqualTo(ProfileType.SALES_MANAGER)
        assertThat(ProfileType.fromSfProfileName("사업부장")).isEqualTo(ProfileType.BUSINESS_DIRECTOR)
        assertThat(ProfileType.fromSfProfileName("본부장")).isEqualTo(ProfileType.DIVISION_HEAD)
        assertThat(ProfileType.fromSfProfileName("5. 영업사원")).isEqualTo(ProfileType.SALES_REP)
    }

    @Test
    @DisplayName("fromSfProfileName — normalized (prefix 없는) 형태 매칭")
    fun fromSfProfileNameNormalized() {
        assertThat(ProfileType.fromSfProfileName("마케팅")).isEqualTo(ProfileType.MARKETING)
        assertThat(ProfileType.fromSfProfileName("Staff")).isEqualTo(ProfileType.STAFF)
        assertThat(ProfileType.fromSfProfileName("조장")).isEqualTo(ProfileType.TEAM_LEADER)
        assertThat(ProfileType.fromSfProfileName("지점장")).isEqualTo(ProfileType.BRANCH_MANAGER)
        assertThat(ProfileType.fromSfProfileName("영업사원")).isEqualTo(ProfileType.SALES_REP)
    }

    @Test
    @DisplayName("fromSfProfileName — SYSTEM_ADMIN i18n 3개 명칭 통합 매핑")
    fun fromSfProfileNameSystemAdminI18n() {
        assertThat(ProfileType.fromSfProfileName("시스템 관리자")).isEqualTo(ProfileType.SYSTEM_ADMIN)
        assertThat(ProfileType.fromSfProfileName("System Administrator")).isEqualTo(ProfileType.SYSTEM_ADMIN)
        assertThat(ProfileType.fromSfProfileName("システム管理者")).isEqualTo(ProfileType.SYSTEM_ADMIN)
    }

    @Test
    @DisplayName("fromSfProfileName — 미매칭 값 → STAFF 디폴트 (SF AppointmentTriggerHandler silently skip 동등)")
    fun fromSfProfileNameUnknownFallback() {
        assertThat(ProfileType.fromSfProfileName("운영자가 추가한 새 Profile")).isEqualTo(ProfileType.STAFF)
        assertThat(ProfileType.fromSfProfileName(null)).isEqualTo(ProfileType.STAFF)
        assertThat(ProfileType.fromSfProfileName("")).isEqualTo(ProfileType.STAFF)
    }

    @Test
    @DisplayName("DB value 는 enum name 과 동일 (VARCHAR(40) 호환)")
    fun valueConvention() {
        ProfileType.entries.forEach { type ->
            assertThat(type.value).isEqualTo(type.name)
            assertThat(type.value.length).isLessThanOrEqualTo(40)
        }
    }
}
