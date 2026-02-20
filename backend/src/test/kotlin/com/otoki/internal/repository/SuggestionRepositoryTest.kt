package com.otoki.internal.repository

import com.otoki.internal.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("SuggestionRepository 테스트")
class SuggestionRepositoryTest {

    @Autowired
    private lateinit var suggestionRepository: SuggestionRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser1: User
    private lateinit var testUser2: User

    @BeforeEach
    fun setUp() {
        suggestionRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        testUser1 = testEntityManager.persistAndFlush(
            User(
                employeeId = "10000001",
                password = "encoded",
                name = "홍길동",
                orgName = "서울지점"
            )
        )

        testUser2 = testEntityManager.persistAndFlush(
            User(
                employeeId = "10000002",
                password = "encoded",
                name = "김영희",
                orgName = "부산지점"
            )
        )
    }

    @Test
    @DisplayName("신제품 제안을 저장할 수 있다")
    fun saveNewProductSuggestion() {
        // Given
        val suggestion = Suggestion(
            user = testUser1,
            category = SuggestionCategory.NEW_PRODUCT,
            productCode = null,
            productName = null,
            title = "저당 라면 시리즈 출시 제안",
            content = "건강을 생각하는 소비자들을 위한 저당 라면 제품군을 제안합니다.",
            status = SuggestionStatus.SUBMITTED
        )

        // When
        val saved = suggestionRepository.save(suggestion)

        // Then
        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.user.id).isEqualTo(testUser1.id)
        assertThat(saved.category).isEqualTo(SuggestionCategory.NEW_PRODUCT)
        assertThat(saved.productCode).isNull()
        assertThat(saved.productName).isNull()
        assertThat(saved.title).isEqualTo("저당 라면 시리즈 출시 제안")
        assertThat(saved.status).isEqualTo(SuggestionStatus.SUBMITTED)
    }

    @Test
    @DisplayName("기존제품 개선 제안을 저장할 수 있다")
    fun saveExistingProductSuggestion() {
        // Given
        val suggestion = Suggestion(
            user = testUser1,
            category = SuggestionCategory.EXISTING_PRODUCT,
            productCode = "PROD001",
            productName = "진라면",
            title = "진라면 컵라면 용기 개선 제안",
            content = "용기를 더 견고하게 만들어 운반 시 찌그러짐을 방지할 수 있습니다.",
            status = SuggestionStatus.SUBMITTED
        )

        // When
        val saved = suggestionRepository.save(suggestion)

        // Then
        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.user.id).isEqualTo(testUser1.id)
        assertThat(saved.category).isEqualTo(SuggestionCategory.EXISTING_PRODUCT)
        assertThat(saved.productCode).isEqualTo("PROD001")
        assertThat(saved.productName).isEqualTo("진라면")
        assertThat(saved.title).isEqualTo("진라면 컵라면 용기 개선 제안")
        assertThat(saved.status).isEqualTo(SuggestionStatus.SUBMITTED)
    }

    @Test
    @DisplayName("사용자별 제안 목록을 최신순으로 조회할 수 있다")
    fun findByUserIdOrderByCreatedAtDesc() {
        // Given
        val suggestion1 = suggestionRepository.save(
            Suggestion(
                user = testUser1,
                category = SuggestionCategory.NEW_PRODUCT,
                title = "첫 번째 제안",
                content = "첫 번째 제안 내용",
                status = SuggestionStatus.SUBMITTED
            )
        )

        Thread.sleep(10) // createdAt 차이를 위한 대기

        val suggestion2 = suggestionRepository.save(
            Suggestion(
                user = testUser1,
                category = SuggestionCategory.EXISTING_PRODUCT,
                productCode = "PROD001",
                productName = "진라면",
                title = "두 번째 제안",
                content = "두 번째 제안 내용",
                status = SuggestionStatus.SUBMITTED
            )
        )

        // user2의 제안 (조회 대상 아님)
        suggestionRepository.save(
            Suggestion(
                user = testUser2,
                category = SuggestionCategory.NEW_PRODUCT,
                title = "다른 사용자 제안",
                content = "다른 사용자 제안 내용",
                status = SuggestionStatus.SUBMITTED
            )
        )

        // When
        val suggestions = suggestionRepository.findByUserIdOrderByCreatedAtDesc(testUser1.id)

        // Then
        assertThat(suggestions).hasSize(2)
        assertThat(suggestions[0].id).isEqualTo(suggestion2.id) // 최신순
        assertThat(suggestions[1].id).isEqualTo(suggestion1.id)
    }
}
