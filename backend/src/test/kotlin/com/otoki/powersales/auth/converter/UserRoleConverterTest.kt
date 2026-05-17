package com.otoki.powersales.auth.converter

import com.otoki.powersales.auth.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * UserRoleConverter 양방향 매핑 검증.
 *
 * Spec #573 P1-B 정책 — DB 저장은 `UserRole.name` (영문 enum.name).
 */
@DisplayName("UserRoleConverter")
class UserRoleConverterTest {

    private val converter = UserRoleConverter()

    @Nested
    @DisplayName("convertToDatabaseColumn — Entity → DB (enum.name 저장)")
    inner class ToDb {

        @Test
        @DisplayName("LEADER → 'LEADER'")
        fun leader() {
            assertThat(converter.convertToDatabaseColumn(UserRole.LEADER)).isEqualTo("LEADER")
        }

        @Test
        @DisplayName("WOMAN → 'WOMAN'")
        fun woman() {
            assertThat(converter.convertToDatabaseColumn(UserRole.WOMAN)).isEqualTo("WOMAN")
        }

        @Test
        @DisplayName("SYSTEM_ADMIN → 'SYSTEM_ADMIN'")
        fun systemAdmin() {
            assertThat(converter.convertToDatabaseColumn(UserRole.SYSTEM_ADMIN)).isEqualTo("SYSTEM_ADMIN")
        }

        @Test
        @DisplayName("ACCOUNT_VIEW_ALL → 'ACCOUNT_VIEW_ALL'")
        fun accountViewAll() {
            assertThat(converter.convertToDatabaseColumn(UserRole.ACCOUNT_VIEW_ALL)).isEqualTo("ACCOUNT_VIEW_ALL")
        }

        @Test
        @DisplayName("null → null")
        fun nullAttribute() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull()
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute — DB → Entity (enum.valueOf)")
    inner class ToEntity {

        @Test
        @DisplayName("'LEADER' → LEADER")
        fun leader() {
            assertThat(converter.convertToEntityAttribute("LEADER")).isEqualTo(UserRole.LEADER)
        }

        @Test
        @DisplayName("'SYSTEM_ADMIN' → SYSTEM_ADMIN")
        fun systemAdmin() {
            assertThat(converter.convertToEntityAttribute("SYSTEM_ADMIN")).isEqualTo(UserRole.SYSTEM_ADMIN)
        }

        @Test
        @DisplayName("'ACCOUNT_VIEW_ALL' → ACCOUNT_VIEW_ALL")
        fun accountViewAll() {
            assertThat(converter.convertToEntityAttribute("ACCOUNT_VIEW_ALL")).isEqualTo(UserRole.ACCOUNT_VIEW_ALL)
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
            assertThat(converter.convertToEntityAttribute("INVALID_VALUE")).isEqualTo(UserRole.UNKNOWN)
        }
    }
}
