package com.otoki.powersales.auth.converter

import com.otoki.powersales.auth.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * UserRoleConverter 양방향 매핑 검증.
 *
 * SF Object 정합 정책 (`sf-object-meta/sandbox/README.md` §6.6 v2.2) — DB 저장은 SF picklist 원본값.
 * SF `DKRetail__AppAuthority__c` 옵션: `조장` / `여사원` / `지점장` / `AccountViewAll`.
 */
@DisplayName("UserRoleConverter")
class UserRoleConverterTest {

    private val converter = UserRoleConverter()

    @Nested
    @DisplayName("convertToDatabaseColumn — Entity → DB (SF 원본값 저장)")
    inner class ToDb {

        @Test
        @DisplayName("LEADER → '조장' (SF 원본 한글값)")
        fun leader() {
            assertThat(converter.convertToDatabaseColumn(UserRole.LEADER)).isEqualTo("조장")
        }

        @Test
        @DisplayName("WOMAN → '여사원'")
        fun woman() {
            assertThat(converter.convertToDatabaseColumn(UserRole.WOMAN)).isEqualTo("여사원")
        }

        @Test
        @DisplayName("BRANCH_MANAGER → '지점장'")
        fun branchManager() {
            assertThat(converter.convertToDatabaseColumn(UserRole.BRANCH_MANAGER)).isEqualTo("지점장")
        }

        @Test
        @DisplayName("ACCOUNT_VIEW_ALL → 'AccountViewAll' (SF 영문 원본값)")
        fun accountViewAll() {
            assertThat(converter.convertToDatabaseColumn(UserRole.ACCOUNT_VIEW_ALL)).isEqualTo("AccountViewAll")
        }

        @Test
        @DisplayName("null → null")
        fun nullAttribute() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute — DB → Entity")
    inner class ToEntity {

        @Test
        @DisplayName("'조장' → LEADER")
        fun leader() {
            assertThat(converter.convertToEntityAttribute("조장")).isEqualTo(UserRole.LEADER)
        }

        @Test
        @DisplayName("'AccountViewAll' → ACCOUNT_VIEW_ALL")
        fun accountViewAll() {
            assertThat(converter.convertToEntityAttribute("AccountViewAll")).isEqualTo(UserRole.ACCOUNT_VIEW_ALL)
        }

        @Test
        @DisplayName("null → null")
        fun nullDb() {
            assertThat(converter.convertToEntityAttribute(null)).isNull()
        }

        @Test
        @DisplayName("빈 문자열 → null")
        fun emptyDb() {
            assertThat(converter.convertToEntityAttribute("")).isNull()
        }

        @Test
        @DisplayName("미지의 값 → UNKNOWN fallback")
        fun unknown() {
            assertThat(converter.convertToEntityAttribute("정체불명")).isEqualTo(UserRole.UNKNOWN)
        }
    }
}
