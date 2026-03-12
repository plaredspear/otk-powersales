package com.otoki.internal.promotion.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StandLocation Enum 테스트")
class StandLocationTest {

    @Nested
    @DisplayName("entries - Enum 값 목록")
    inner class EntriesTests {

        @Test
        @DisplayName("6개 값이 정의됨")
        fun entries_hasSixValues() {
            assertThat(StandLocation.entries).hasSize(6)
        }

        @Test
        @DisplayName("displayOrder 순서대로 정렬됨")
        fun entries_orderedByDisplayOrder() {
            val ordered = StandLocation.entries.sortedBy { it.displayOrder }
            assertThat(ordered.map { it.displayName }).containsExactly(
                "냉동행사장", "아일랜드", "엔드", "평대", "푸드트럭", "행사매대"
            )
        }
    }

    @Nested
    @DisplayName("fromDisplayName - 표시명으로 Enum 조회")
    inner class FromDisplayNameTests {

        @Test
        @DisplayName("유효한 표시명 -> 해당 Enum 반환")
        fun fromDisplayName_valid() {
            assertThat(StandLocation.fromDisplayName("냉동행사장")).isEqualTo(StandLocation.FROZEN_EVENT)
            assertThat(StandLocation.fromDisplayName("행사매대")).isEqualTo(StandLocation.EVENT_STAND)
        }

        @Test
        @DisplayName("유효하지 않은 표시명 -> null 반환")
        fun fromDisplayName_invalid() {
            assertThat(StandLocation.fromDisplayName("없는매대")).isNull()
            assertThat(StandLocation.fromDisplayName("")).isNull()
        }
    }
}
