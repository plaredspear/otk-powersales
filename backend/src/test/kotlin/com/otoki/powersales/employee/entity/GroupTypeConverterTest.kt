package com.otoki.powersales.employee.entity

import com.otoki.powersales.employee.entity.converter.GroupTypeConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #755 — GroupTypeConverter 양방향 매핑 + 미지 옵션값 null 반환 검증.
 */
@DisplayName("GroupTypeConverter")
class GroupTypeConverterTest {

    private val converter = GroupTypeConverter()

    @Nested
    @DisplayName("convertToDatabaseColumn — Entity → DB")
    inner class ToDb {

        @Test
        @DisplayName("Regular → 'Regular'")
        fun regular() {
            assertThat(converter.convertToDatabaseColumn(GroupType.REGULAR)).isEqualTo("Regular")
        }

        @Test
        @DisplayName("Queue → 'Queue'")
        fun queue() {
            assertThat(converter.convertToDatabaseColumn(GroupType.QUEUE)).isEqualTo("Queue")
        }

        @Test
        @DisplayName("ManagerAndSubordinatesInternal → SF 원본값 보존")
        fun managerAndSubordinatesInternal() {
            assertThat(converter.convertToDatabaseColumn(GroupType.MANAGER_AND_SUBORDINATES_INTERNAL))
                .isEqualTo("ManagerAndSubordinatesInternal")
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
        @DisplayName("'Regular' → GroupType.REGULAR")
        fun regular() {
            assertThat(converter.convertToEntityAttribute("Regular")).isEqualTo(GroupType.REGULAR)
        }

        @Test
        @DisplayName("'Queue' → GroupType.QUEUE")
        fun queue() {
            assertThat(converter.convertToEntityAttribute("Queue")).isEqualTo(GroupType.QUEUE)
        }

        @Test
        @DisplayName("'RoleAndSubordinatesInternal' → GroupType.ROLE_AND_SUBORDINATES_INTERNAL")
        fun roleAndSubordinatesInternal() {
            assertThat(converter.convertToEntityAttribute("RoleAndSubordinatesInternal"))
                .isEqualTo(GroupType.ROLE_AND_SUBORDINATES_INTERNAL)
        }

        @Test
        @DisplayName("미지 옵션값 → null (경고 로그)")
        fun unknownValue() {
            assertThat(converter.convertToEntityAttribute("UnknownType")).isNull()
        }

        @Test
        @DisplayName("null → null")
        fun nullDbData() {
            assertThat(converter.convertToEntityAttribute(null)).isNull()
        }

        @Test
        @DisplayName("빈 문자열 → null")
        fun emptyDbData() {
            assertThat(converter.convertToEntityAttribute("")).isNull()
        }
    }

    @Nested
    @DisplayName("GroupType enum 자체")
    inner class EnumProperties {

        @Test
        @DisplayName("17개 옵션 (SF picklist 정합)")
        fun seventeenOptions() {
            assertThat(GroupType.entries).hasSize(17)
        }

        @Test
        @DisplayName("fromDisplayName 정상값 — Role")
        fun fromDisplayName_role() {
            assertThat(GroupType.fromDisplayName("Role")).isEqualTo(GroupType.ROLE)
        }

        @Test
        @DisplayName("fromDisplayName 미지값 → IllegalArgumentException")
        fun fromDisplayName_invalid() {
            org.assertj.core.api.Assertions.assertThatThrownBy {
                GroupType.fromDisplayName("UnknownType")
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("fromDisplayNameOrNull — null/blank → null")
        fun fromDisplayNameOrNull_nullBlank() {
            assertThat(GroupType.fromDisplayNameOrNull(null)).isNull()
            assertThat(GroupType.fromDisplayNameOrNull("")).isNull()
            assertThat(GroupType.fromDisplayNameOrNull("   ")).isNull()
        }
    }
}
