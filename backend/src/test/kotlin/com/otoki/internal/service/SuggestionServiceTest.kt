package com.otoki.internal.service

import com.otoki.internal.dto.request.SuggestionCreateRequest
import com.otoki.internal.entity.*
import com.otoki.internal.exception.*
import com.otoki.internal.repository.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("SuggestionService 테스트")
class SuggestionServiceTest {

    @Mock
    private lateinit var suggestionRepository: SuggestionRepository

    @Mock
    private lateinit var suggestionPhotoRepository: SuggestionPhotoRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @InjectMocks
    private lateinit var suggestionService: SuggestionService

    // ========== 제안 등록 Tests ==========

    @Nested
    @DisplayName("제안 등록")
    inner class CreateSuggestion {

        @Test
        @DisplayName("성공 - 신제품 제안 (사진 없음)")
        fun createSuggestion_newProduct_withoutPhotos_success() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "NEW_PRODUCT",
                productCode = null,
                title = "저당 라면 시리즈 출시 제안",
                content = "건강을 생각하는 소비자들을 위한 저당 라면을 제안합니다."
            )

            val mockUser = createMockUser(userId)
            val savedSuggestion = createMockSuggestion(1L, mockUser, SuggestionCategory.NEW_PRODUCT)

            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)
            doReturn(savedSuggestion).whenever(suggestionRepository).save(any())

            // When
            val result = suggestionService.createSuggestion(
                userId = userId,
                request = request,
                photos = null
            )

            // Then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.category).isEqualTo("NEW_PRODUCT")
            assertThat(result.categoryName).isEqualTo("신제품 제안")
            assertThat(result.productCode).isNull()
            assertThat(result.productName).isNull()
            assertThat(result.title).isEqualTo("테스트 제안")
            verify(suggestionRepository).save(any())
            verify(suggestionPhotoRepository, never()).saveAll(any<List<SuggestionPhoto>>())
        }

        @Test
        @DisplayName("성공 - 신제품 제안 (사진 2장 포함)")
        fun createSuggestion_newProduct_withPhotos_success() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "NEW_PRODUCT",
                productCode = null,
                title = "저당 라면 시리즈",
                content = "제안 내용"
            )
            val photos = listOf(
                createMockImageFile("photo1.jpg"),
                createMockImageFile("photo2.jpg")
            )

            val mockUser = createMockUser(userId)
            val savedSuggestion = createMockSuggestion(1L, mockUser, SuggestionCategory.NEW_PRODUCT)

            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)
            doReturn(savedSuggestion).whenever(suggestionRepository).save(any())
            doReturn("http://example.com/photo1.jpg").whenever(fileStorageService)
                .uploadSuggestionPhoto(any(), any(), any(), eq(0))
            doReturn("http://example.com/photo2.jpg").whenever(fileStorageService)
                .uploadSuggestionPhoto(any(), any(), any(), eq(1))

            // When
            val result = suggestionService.createSuggestion(
                userId = userId,
                request = request,
                photos = photos
            )

            // Then
            assertThat(result.id).isEqualTo(1L)
            verify(suggestionRepository).save(any())
            verify(suggestionPhotoRepository).saveAll(any<List<SuggestionPhoto>>())
            verify(fileStorageService, times(2)).uploadSuggestionPhoto(any(), any(), any(), any())
        }

        @Test
        @DisplayName("성공 - 기존제품 개선 제안")
        fun createSuggestion_existingProduct_success() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "EXISTING_PRODUCT",
                productCode = "PROD001",
                title = "진라면 용기 개선",
                content = "용기를 더 견고하게 개선할 것을 제안합니다."
            )

            val mockUser = createMockUser(userId)
            val mockProduct = createMockProduct("PROD001", "진라면")
            val savedSuggestion = createMockSuggestion(
                id = 1L,
                user = mockUser,
                category = SuggestionCategory.EXISTING_PRODUCT,
                productCode = "PROD001",
                productName = "진라면"
            )

            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)
            doReturn(mockProduct).whenever(productRepository).findByProductCode("PROD001")
            doReturn(savedSuggestion).whenever(suggestionRepository).save(any())

            // When
            val result = suggestionService.createSuggestion(
                userId = userId,
                request = request,
                photos = null
            )

            // Then
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.category).isEqualTo("EXISTING_PRODUCT")
            assertThat(result.categoryName).isEqualTo("기존제품 상품가치향상")
            assertThat(result.productCode).isEqualTo("PROD001")
            assertThat(result.productName).isEqualTo("진라면")
            verify(suggestionRepository).save(any())
            verify(productRepository).findByProductCode("PROD001")
        }

        @Test
        @DisplayName("실패 - 잘못된 분류")
        fun createSuggestion_invalidCategory_throwsException() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "INVALID_CATEGORY",
                productCode = null,
                title = "제안",
                content = "내용"
            )

            val mockUser = createMockUser(userId)
            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)

            // When & Then
            assertThrows<InvalidCategoryException> {
                suggestionService.createSuggestion(userId, request, null)
            }
        }

        @Test
        @DisplayName("실패 - 기존제품인데 제품 코드 없음")
        fun createSuggestion_existingProductWithoutCode_throwsException() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "EXISTING_PRODUCT",
                productCode = null,
                title = "제안",
                content = "내용"
            )

            val mockUser = createMockUser(userId)
            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)

            // When & Then
            assertThrows<ProductRequiredForExistingException> {
                suggestionService.createSuggestion(userId, request, null)
            }
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 제품")
        fun createSuggestion_productNotFound_throwsException() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "EXISTING_PRODUCT",
                productCode = "NONEXISTENT",
                title = "제안",
                content = "내용"
            )

            val mockUser = createMockUser(userId)
            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)
            doReturn(null).whenever(productRepository).findByProductCode("NONEXISTENT")

            // When & Then
            assertThrows<ProductNotFoundException> {
                suggestionService.createSuggestion(userId, request, null)
            }
        }

        @Test
        @DisplayName("실패 - 사진 개수 초과 (3장)")
        fun createSuggestion_tooManyPhotos_throwsException() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "NEW_PRODUCT",
                productCode = null,
                title = "제안",
                content = "내용"
            )
            val photos = listOf(
                createMockImageFile("photo1.jpg"),
                createMockImageFile("photo2.jpg"),
                createMockImageFile("photo3.jpg")
            )

            val mockUser = createMockUser(userId)
            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)

            // When & Then
            assertThrows<PhotoCountExceededException> {
                suggestionService.createSuggestion(userId, request, photos)
            }
        }

        @Test
        @DisplayName("실패 - 사진 크기 초과 (15MB)")
        fun createSuggestion_photoTooLarge_throwsException() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "NEW_PRODUCT",
                productCode = null,
                title = "제안",
                content = "내용"
            )
            val photos = listOf(
                MockMultipartFile(
                    "photo",
                    "large.jpg",
                    "image/jpeg",
                    ByteArray(15 * 1024 * 1024)
                )
            )

            val mockUser = createMockUser(userId)
            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)

            // When & Then
            assertThrows<PhotoSizeExceededException> {
                suggestionService.createSuggestion(userId, request, photos)
            }
        }

        @Test
        @DisplayName("실패 - 잘못된 파일 형식 (text/plain)")
        fun createSuggestion_invalidFileType_throwsException() {
            // Given
            val userId = 1L
            val request = SuggestionCreateRequest(
                category = "NEW_PRODUCT",
                productCode = null,
                title = "제안",
                content = "내용"
            )
            val photos = listOf(
                MockMultipartFile(
                    "file",
                    "document.txt",
                    "text/plain",
                    "text content".toByteArray()
                )
            )

            val mockUser = createMockUser(userId)
            doReturn(Optional.of(mockUser)).whenever(userRepository).findById(userId)

            // When & Then
            assertThrows<InvalidPhotoException> {
                suggestionService.createSuggestion(userId, request, photos)
            }
        }
    }

    // ========== Helper Methods ==========

    private fun createMockUser(id: Long): User {
        return User(
            id = id,
            employeeId = "10000001",
            password = "encoded",
            name = "홍길동",
            department = "영업부",
            branchName = "서울지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL
        )
    }

    private fun createMockProduct(code: String, name: String): Product {
        return Product(
            id = 1L,
            productId = code,
            productName = name,
            productCode = code,
            barcode = "8801234567890",
            storageType = "상온"
        )
    }

    private fun createMockSuggestion(
        id: Long,
        user: User,
        category: SuggestionCategory,
        productCode: String? = null,
        productName: String? = null
    ): Suggestion {
        return Suggestion(
            id = id,
            user = user,
            category = category,
            productCode = productCode,
            productName = productName,
            title = "테스트 제안",
            content = "테스트 내용",
            status = SuggestionStatus.SUBMITTED
        )
    }

    private fun createMockImageFile(filename: String): MockMultipartFile {
        return MockMultipartFile(
            "photo",
            filename,
            "image/jpeg",
            ByteArray(1024)
        )
    }
}
