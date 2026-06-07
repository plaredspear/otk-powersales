package com.otoki.powersales.notice.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("NoticeCategory 테스트")
class NoticeCategoryTest {

    @Nested
    @DisplayName("homeDisplayName - 홈 화면 2분류 라벨")
    inner class HomeDisplayName {

        @Test
        @DisplayName("회사공지 -> 전체 공지")
        fun company_total() {
            assertThat(NoticeCategory.COMPANY.homeDisplayName).isEqualTo("전체 공지")
        }

        @Test
        @DisplayName("교육 -> 전체 공지")
        fun education_total() {
            assertThat(NoticeCategory.EDUCATION.homeDisplayName).isEqualTo("전체 공지")
        }

        @Test
        @DisplayName("영업부/지점공지 -> 지점 공지")
        fun branch_branch() {
            assertThat(NoticeCategory.BRANCH.homeDisplayName).isEqualTo("지점 공지")
        }
    }
}
