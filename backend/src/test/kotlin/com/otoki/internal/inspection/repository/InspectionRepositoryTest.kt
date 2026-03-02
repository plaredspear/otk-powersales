package com.otoki.internal.inspection.repository

import com.otoki.internal.common.config.QueryDslConfig
import com.otoki.internal.inspection.entity.InspectionTheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("InspectionThemeRepository 테스트")
class InspectionRepositoryTest {

    @Autowired
    private lateinit var inspectionThemeRepository: InspectionThemeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        inspectionThemeRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("findActiveThemesByDate - 활성 테마 조회")
    inner class FindActiveThemesByDateTests {

        @Test
        @DisplayName("활성 테마 조회 - 날짜 범위 내 공개 테마 반환")
        fun activeThemes_withinDateRange() {
            // Given
            persistTheme(name = "6월 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)
            persistTheme(name = "7월 테마", startDate = LocalDate.of(2026, 7, 1), endDate = LocalDate.of(2026, 7, 31), publicFlag = true)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 6, 15))

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("6월 테마")
        }

        @Test
        @DisplayName("비공개 제외 - publicFlag=false 테마는 결과에 미포함")
        fun excludeNonPublic() {
            // Given
            persistTheme(name = "공개 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)
            persistTheme(name = "비공개 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = false)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 6, 15))

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("공개 테마")
        }

        @Test
        @DisplayName("기간 외 제외 - targetDate가 endDate 이후면 미포함")
        fun excludeExpired() {
            // Given
            persistTheme(name = "만료 테마", startDate = LocalDate.of(2026, 5, 1), endDate = LocalDate.of(2026, 5, 31), publicFlag = true)
            persistTheme(name = "활성 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 6, 15))

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("활성 테마")
        }

        @Test
        @DisplayName("이름순 정렬 - 복수 결과가 name ASC 정렬")
        fun orderedByNameAsc() {
            // Given
            persistTheme(name = "다 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)
            persistTheme(name = "가 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)
            persistTheme(name = "나 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 6, 15))

            // Then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.name }).containsExactly("가 테마", "나 테마", "다 테마")
        }

        @Test
        @DisplayName("결과 없음 - 활성 테마가 없는 날짜면 빈 리스트 반환")
        fun noActiveThemes() {
            // Given
            persistTheme(name = "6월 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 8, 15))

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("경계값 - startDate와 동일한 날짜도 포함")
        fun boundaryStartDate() {
            // Given
            persistTheme(name = "6월 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 6, 1))

            // Then
            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("경계값 - endDate와 동일한 날짜도 포함")
        fun boundaryEndDate() {
            // Given
            persistTheme(name = "6월 테마", startDate = LocalDate.of(2026, 6, 1), endDate = LocalDate.of(2026, 6, 30), publicFlag = true)

            // When
            val result = inspectionThemeRepository.findActiveThemesByDate(LocalDate.of(2026, 6, 30))

            // Then
            assertThat(result).hasSize(1)
        }
    }

    private fun persistTheme(
        name: String = "테스트 테마",
        startDate: LocalDate = LocalDate.of(2026, 6, 1),
        endDate: LocalDate = LocalDate.of(2026, 6, 30),
        publicFlag: Boolean = true
    ): InspectionTheme {
        val theme = InspectionTheme(
            name = name,
            startDate = startDate,
            endDate = endDate,
            publicFlag = publicFlag
        )
        val persisted = testEntityManager.persistAndFlush(theme)
        testEntityManager.clear()
        return persisted
    }
}
