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
@DisplayName("SuggestionPhotoRepository 테스트")
class SuggestionPhotoRepositoryTest {

    @Autowired
    private lateinit var suggestionPhotoRepository: SuggestionPhotoRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser: User
    private lateinit var testSuggestion: Suggestion

    @BeforeEach
    fun setUp() {
        suggestionPhotoRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        testUser = testEntityManager.persistAndFlush(
            User(
                employeeId = "10000001",
                password = "encoded",
                name = "홍길동",
                department = "영업부",
                branchName = "서울지점",
                role = UserRole.USER,
                workerType = WorkerType.PATROL
            )
        )

        // 테스트 제안 생성
        testSuggestion = testEntityManager.persistAndFlush(
            Suggestion(
                user = testUser,
                category = SuggestionCategory.NEW_PRODUCT,
                title = "신제품 제안",
                content = "신제품 제안 내용",
                status = SuggestionStatus.SUBMITTED
            )
        )
    }

    @Test
    @DisplayName("제안 사진을 저장할 수 있다")
    fun saveSuggestionPhoto() {
        // Given
        val photo = SuggestionPhoto(
            suggestion = testSuggestion,
            url = "https://storage.example.com/suggestions/123/0-uuid.jpg",
            originalFileName = "photo1.jpg",
            fileSize = 1024000,
            contentType = "image/jpeg",
            sortOrder = 0
        )

        // When
        val saved = suggestionPhotoRepository.save(photo)

        // Then
        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.suggestion.id).isEqualTo(testSuggestion.id)
        assertThat(saved.url).isEqualTo("https://storage.example.com/suggestions/123/0-uuid.jpg")
        assertThat(saved.originalFileName).isEqualTo("photo1.jpg")
        assertThat(saved.fileSize).isEqualTo(1024000)
        assertThat(saved.contentType).isEqualTo("image/jpeg")
        assertThat(saved.sortOrder).isEqualTo(0)
    }

    @Test
    @DisplayName("한 제안에 여러 사진을 저장할 수 있다")
    fun saveMultipleSuggestionPhotos() {
        // Given
        val photo1 = SuggestionPhoto(
            suggestion = testSuggestion,
            url = "https://storage.example.com/suggestions/123/0-uuid1.jpg",
            originalFileName = "photo1.jpg",
            fileSize = 1024000,
            contentType = "image/jpeg",
            sortOrder = 0
        )

        val photo2 = SuggestionPhoto(
            suggestion = testSuggestion,
            url = "https://storage.example.com/suggestions/123/1-uuid2.jpg",
            originalFileName = "photo2.jpg",
            fileSize = 2048000,
            contentType = "image/png",
            sortOrder = 1
        )

        // When
        val saved1 = suggestionPhotoRepository.save(photo1)
        val saved2 = suggestionPhotoRepository.save(photo2)

        // Then
        assertThat(saved1.id).isGreaterThan(0)
        assertThat(saved2.id).isGreaterThan(0)
        assertThat(saved1.sortOrder).isEqualTo(0)
        assertThat(saved2.sortOrder).isEqualTo(1)
    }
}
