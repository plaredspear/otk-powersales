package com.otoki.internal.service

import com.otoki.internal.dto.request.ClaimCreateRequest
import com.otoki.internal.entity.*
import com.otoki.internal.repository.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("ClaimService 테스트")
class ClaimServiceTest {

    @Mock
    private lateinit var claimRepository: ClaimRepository

    @Mock
    private lateinit var claimPhotoRepository: ClaimPhotoRepository

    @Mock
    private lateinit var claimCategoryRepository: ClaimCategoryRepository

    @Mock
    private lateinit var claimSubcategoryRepository: ClaimSubcategoryRepository

    @Mock
    private lateinit var claimPurchaseMethodRepository: ClaimPurchaseMethodRepository

    @Mock
    private lateinit var claimRequestTypeRepository: ClaimRequestTypeRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @InjectMocks
    private lateinit var claimService: ClaimService

    // ========== 클레임 등록 Tests ==========

    @Nested
    @DisplayName("클레임 등록")
    inner class CreateClaim {

        @Test
        @DisplayName("성공 - 기본 정보만 입력")
        fun createClaim_basicInfo_success() {
            // Given
            val userId = 1L
            val request = createBasicRequest()
            val defectPhoto = createMockFile("defect.jpg")
            val labelPhoto = createMockFile("label.jpg")

            setupMocksForSuccess(userId)

            // When
            val result = claimService.createClaim(
                userId = userId,
                request = request,
                defectPhoto = defectPhoto,
                labelPhoto = labelPhoto,
                receiptPhoto = null
            )

            // Then
            assertThat(result.id).isGreaterThan(0)
            assertThat(result.storeName).isEqualTo("테스트 거래처")
            assertThat(result.productName).isEqualTo("테스트 제품")
            verify(claimRepository).save(any())
            verify(claimPhotoRepository).saveAll(any<List<ClaimPhoto>>())
            verify(fileStorageService, times(2)).uploadClaimPhoto(any(), any(), any(), any())
        }

        @Test
        @DisplayName("성공 - 구매 정보 포함")
        fun createClaim_withPurchaseInfo_success() {
            // Given
            val userId = 1L
            val request = createRequestWithPurchase()
            val defectPhoto = createMockFile("defect.jpg")
            val labelPhoto = createMockFile("label.jpg")
            val receiptPhoto = createMockFile("receipt.jpg")

            setupMocksForSuccess(userId)
            val purchaseMethod = createMockPurchaseMethod()
            doReturn(Optional.of(purchaseMethod)).whenever(claimPurchaseMethodRepository).findById("PM01")

            // When
            val result = claimService.createClaim(
                userId = userId,
                request = request,
                defectPhoto = defectPhoto,
                labelPhoto = labelPhoto,
                receiptPhoto = receiptPhoto
            )

            // Then
            assertThat(result).isNotNull
            verify(claimPhotoRepository).saveAll(any<List<ClaimPhoto>>())
            verify(fileStorageService, times(3)).uploadClaimPhoto(any(), any(), any(), any())
        }

    }

    // ========== 폼 초기화 데이터 조회 Tests ==========

    @Nested
    @DisplayName("폼 초기화 데이터 조회")
    inner class GetFormData {

        @Test
        @DisplayName("성공 - 종류1+종류2 중첩 구조 반환")
        fun getFormData_withNestedStructure() {
            // Given
            val category1 = createMockCategory()
            val category2 = ClaimCategory(id = 2L, name = "변질/변패", sortOrder = 2, isActive = true)
            val subcategories1 = listOf(
                createMockSubcategory(category1, 1L, "벌레"),
                createMockSubcategory(category1, 2L, "금속")
            )
            val subcategories2 = listOf(
                createMockSubcategory(category2, 3L, "곰팡이")
            )

            doReturn(listOf(category1, category2))
                .whenever(claimCategoryRepository).findByIsActiveTrueOrderBySortOrderAsc()
            doReturn(subcategories1)
                .whenever(claimSubcategoryRepository).findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(category1.id)
            doReturn(subcategories2)
                .whenever(claimSubcategoryRepository).findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(category2.id)
            doReturn(listOf(createMockPurchaseMethod()))
                .whenever(claimPurchaseMethodRepository).findByIsActiveTrueOrderBySortOrderAsc()
            doReturn(listOf(createMockRequestType()))
                .whenever(claimRequestTypeRepository).findByIsActiveTrueOrderBySortOrderAsc()

            // When
            val result = claimService.getFormData()

            // Then
            assertThat(result.categories).hasSize(2)
            assertThat(result.categories[0].subcategories).hasSize(2)
            assertThat(result.categories[1].subcategories).hasSize(1)
            assertThat(result.purchaseMethods).hasSize(1)
            assertThat(result.requestTypes).hasSize(1)
        }

        @Test
        @DisplayName("성공 - sortOrder 순 정렬 확인")
        fun getFormData_sortedBySortOrder() {
            // Given
            val category1 = ClaimCategory(id = 1L, name = "이물", sortOrder = 1, isActive = true)
            val category2 = ClaimCategory(id = 2L, name = "변질", sortOrder = 2, isActive = true)

            doReturn(listOf(category1, category2))
                .whenever(claimCategoryRepository).findByIsActiveTrueOrderBySortOrderAsc()
            doReturn(emptyList<ClaimSubcategory>())
                .whenever(claimSubcategoryRepository).findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(any())
            doReturn(emptyList<ClaimPurchaseMethod>())
                .whenever(claimPurchaseMethodRepository).findByIsActiveTrueOrderBySortOrderAsc()
            doReturn(emptyList<ClaimRequestType>())
                .whenever(claimRequestTypeRepository).findByIsActiveTrueOrderBySortOrderAsc()

            // When
            val result = claimService.getFormData()

            // Then
            assertThat(result.categories[0].name).isEqualTo("이물")
            assertThat(result.categories[1].name).isEqualTo("변질")
        }
    }

    // ========== Helper Methods ==========

    private fun createBasicRequest() = ClaimCreateRequest(
        storeId = 1L,
        productCode = "PROD001",
        dateType = "EXPIRY_DATE",
        date = "2026-12-31",
        categoryId = 1L,
        subcategoryId = 1L,
        defectDescription = "불량입니다",
        defectQuantity = 1
    )

    private fun createRequestWithPurchase() = ClaimCreateRequest(
        storeId = 1L,
        productCode = "PROD001",
        dateType = "EXPIRY_DATE",
        date = "2026-12-31",
        categoryId = 1L,
        subcategoryId = 1L,
        defectDescription = "불량입니다",
        defectQuantity = 1,
        purchaseAmount = 10000,
        purchaseMethodCode = "PM01"
    )

    private fun createMockFile(filename: String) = MockMultipartFile(
        "file",
        filename,
        "image/jpeg",
        ByteArray(100)
    )

    private fun setupMocksForSuccess(userId: Long) {
        val user = createMockUser()
        val store = createMockStore()
        val product = createMockProduct()
        val category = createMockCategory()
        val subcategory = createMockSubcategory(category, 1L, "벌레")
        val claim = createMockClaim(user, store, product, category, subcategory)

        doReturn(Optional.of(user)).whenever(userRepository).findById(userId)
        doReturn(Optional.of(store)).whenever(storeRepository).findById(any())
        doReturn(product).whenever(productRepository).findByProductCode(any())
        doReturn(Optional.of(category)).whenever(claimCategoryRepository).findById(any())
        doReturn(Optional.of(subcategory)).whenever(claimSubcategoryRepository).findById(any())
        doReturn(claim).whenever(claimRepository).save(any())
        doReturn(emptyList<ClaimPhoto>()).whenever(claimPhotoRepository).saveAll(any<List<ClaimPhoto>>())
        doReturn("http://localhost/files/claims/1/1/defect.jpg")
            .whenever(fileStorageService).uploadClaimPhoto(any(), any(), any(), any())
    }

    private fun createMockUser() = User(
        id = 1L,
        employeeId = "10000001",
        password = "encoded",
        name = "테스트",
        orgName = "서울"
    )

    private fun createMockStore() = Store(
        id = 1L,
        storeCode = "ST001",
        storeName = "테스트 거래처"
    )

    private fun createMockProduct() = Product(
        id = 1L,
        productId = "P001",
        productName = "테스트 제품",
        productCode = "PROD001",
        barcode = "1234567890",
        storageType = "상온"
    )

    private fun createMockCategory() = ClaimCategory(
        id = 1L,
        name = "이물",
        sortOrder = 1,
        isActive = true
    )

    private fun createMockSubcategory(category: ClaimCategory, id: Long, name: String) =
        ClaimSubcategory(
            id = id,
            category = category,
            name = name,
            sortOrder = 1,
            isActive = true
        )

    private fun createMockPurchaseMethod() = ClaimPurchaseMethod(
        code = "PM01",
        name = "대형마트",
        sortOrder = 1,
        isActive = true
    )

    private fun createMockRequestType() = ClaimRequestType(
        code = "RT01",
        name = "교환",
        sortOrder = 1,
        isActive = true
    )

    private fun createMockClaim(
        user: User,
        store: Store,
        product: Product,
        category: ClaimCategory,
        subcategory: ClaimSubcategory
    ) = Claim(
        id = 1L,
        user = user,
        store = store,
        storeName = store.storeName,
        productCode = product.productCode,
        productName = product.productName,
        dateType = ClaimDateType.EXPIRY_DATE,
        date = LocalDate.of(2026, 12, 31),
        category = category,
        subcategory = subcategory,
        defectDescription = "불량입니다",
        defectQuantity = 1,
        status = ClaimStatus.SUBMITTED
    )
}
