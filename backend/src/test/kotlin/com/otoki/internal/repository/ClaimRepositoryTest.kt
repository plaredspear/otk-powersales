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
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("ClaimRepository 테스트")
class ClaimRepositoryTest {

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @Autowired
    private lateinit var claimCategoryRepository: ClaimCategoryRepository

    @Autowired
    private lateinit var claimSubcategoryRepository: ClaimSubcategoryRepository

    @Autowired
    private lateinit var claimPurchaseMethodRepository: ClaimPurchaseMethodRepository

    @Autowired
    private lateinit var claimRequestTypeRepository: ClaimRequestTypeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser1: User
    private lateinit var testUser2: User
    private lateinit var testStore1: Store
    private lateinit var testStore2: Store
    private lateinit var testCategory1: ClaimCategory
    private lateinit var testCategory2: ClaimCategory
    private lateinit var testSubcategory1: ClaimSubcategory
    private lateinit var testSubcategory2: ClaimSubcategory

    @BeforeEach
    fun setUp() {
        claimRepository.deleteAll()
        claimCategoryRepository.deleteAll()
        claimSubcategoryRepository.deleteAll()
        claimPurchaseMethodRepository.deleteAll()
        claimRequestTypeRepository.deleteAll()
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

        // 테스트 거래처 생성
        testStore1 = testEntityManager.persistAndFlush(
            Store(
                storeCode = "ST001",
                storeName = "롯데마트 사상"
            )
        )

        testStore2 = testEntityManager.persistAndFlush(
            Store(
                storeCode = "ST002",
                storeName = "이마트 트레이더스"
            )
        )

        // 테스트 클레임 종류1 생성
        testCategory1 = testEntityManager.persistAndFlush(
            ClaimCategory(
                name = "이물",
                sortOrder = 1,
                isActive = true
            )
        )

        testCategory2 = testEntityManager.persistAndFlush(
            ClaimCategory(
                name = "변질/변패",
                sortOrder = 2,
                isActive = false
            )
        )

        // 테스트 클레임 종류2 생성
        testSubcategory1 = testEntityManager.persistAndFlush(
            ClaimSubcategory(
                category = testCategory1,
                name = "벌레",
                sortOrder = 1,
                isActive = true
            )
        )

        testSubcategory2 = testEntityManager.persistAndFlush(
            ClaimSubcategory(
                category = testCategory1,
                name = "금속",
                sortOrder = 2,
                isActive = true
            )
        )

        // 테스트 구매 방법 생성
        testEntityManager.persistAndFlush(
            ClaimPurchaseMethod(
                code = "PM01",
                name = "대형마트",
                sortOrder = 1,
                isActive = true
            )
        )

        testEntityManager.persistAndFlush(
            ClaimPurchaseMethod(
                code = "PM02",
                name = "편의점",
                sortOrder = 2,
                isActive = false
            )
        )

        // 테스트 요청사항 생성
        testEntityManager.persistAndFlush(
            ClaimRequestType(
                code = "RT01",
                name = "교환",
                sortOrder = 1,
                isActive = true
            )
        )

        testEntityManager.persistAndFlush(
            ClaimRequestType(
                code = "RT02",
                name = "환불",
                sortOrder = 2,
                isActive = true
            )
        )
    }

    @Test
    @DisplayName("클레임을 저장할 수 있다")
    fun saveClaim() {
        // Given
        val claim = Claim(
            user = testUser1,
            store = testStore1,
            storeName = testStore1.storeName,
            productCode = "PROD001",
            productName = "진라면",
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.of(2026, 12, 31),
            category = testCategory1,
            subcategory = testSubcategory1,
            defectDescription = "벌레가 발견되었습니다",
            defectQuantity = 2,
            status = ClaimStatus.SUBMITTED
        )

        // When
        val saved = claimRepository.save(claim)

        // Then
        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.user.id).isEqualTo(testUser1.id)
        assertThat(saved.store.id).isEqualTo(testStore1.id)
        assertThat(saved.productCode).isEqualTo("PROD001")
        assertThat(saved.status).isEqualTo(ClaimStatus.SUBMITTED)
    }

    @Test
    @DisplayName("사용자별 클레임 목록을 최신순으로 조회할 수 있다")
    fun findByUserIdOrderByCreatedAtDesc() {
        // Given
        val claim1 = claimRepository.save(
            Claim(
                user = testUser1,
                store = testStore1,
                storeName = testStore1.storeName,
                productCode = "PROD001",
                productName = "진라면",
                dateType = ClaimDateType.EXPIRY_DATE,
                date = LocalDate.of(2026, 12, 31),
                category = testCategory1,
                subcategory = testSubcategory1,
                defectDescription = "벌레 발견",
                defectQuantity = 1,
                status = ClaimStatus.SUBMITTED
            )
        )

        Thread.sleep(10) // createdAt 차이를 위한 대기

        val claim2 = claimRepository.save(
            Claim(
                user = testUser1,
                store = testStore2,
                storeName = testStore2.storeName,
                productCode = "PROD002",
                productName = "짜파게티",
                dateType = ClaimDateType.MANUFACTURE_DATE,
                date = LocalDate.of(2026, 1, 1),
                category = testCategory1,
                subcategory = testSubcategory2,
                defectDescription = "금속 발견",
                defectQuantity = 1,
                status = ClaimStatus.SUBMITTED
            )
        )

        // user2의 클레임 (조회 대상 아님)
        claimRepository.save(
            Claim(
                user = testUser2,
                store = testStore1,
                storeName = testStore1.storeName,
                productCode = "PROD003",
                productName = "신라면",
                dateType = ClaimDateType.EXPIRY_DATE,
                date = LocalDate.of(2026, 12, 31),
                category = testCategory1,
                subcategory = testSubcategory1,
                defectDescription = "변질",
                defectQuantity = 1,
                status = ClaimStatus.SUBMITTED
            )
        )

        // When
        val claims = claimRepository.findByUserIdOrderByCreatedAtDesc(testUser1.id)

        // Then
        assertThat(claims).hasSize(2)
        assertThat(claims[0].id).isEqualTo(claim2.id) // 최신순
        assertThat(claims[1].id).isEqualTo(claim1.id)
    }

    @Test
    @DisplayName("거래처별 클레임 목록을 최신순으로 조회할 수 있다")
    fun findByStoreIdOrderByCreatedAtDesc() {
        // Given
        val claim1 = claimRepository.save(
            Claim(
                user = testUser1,
                store = testStore1,
                storeName = testStore1.storeName,
                productCode = "PROD001",
                productName = "진라면",
                dateType = ClaimDateType.EXPIRY_DATE,
                date = LocalDate.of(2026, 12, 31),
                category = testCategory1,
                subcategory = testSubcategory1,
                defectDescription = "벌레 발견",
                defectQuantity = 1,
                status = ClaimStatus.SUBMITTED
            )
        )

        Thread.sleep(10)

        val claim2 = claimRepository.save(
            Claim(
                user = testUser2,
                store = testStore1,
                storeName = testStore1.storeName,
                productCode = "PROD002",
                productName = "짜파게티",
                dateType = ClaimDateType.MANUFACTURE_DATE,
                date = LocalDate.of(2026, 1, 1),
                category = testCategory1,
                subcategory = testSubcategory2,
                defectDescription = "금속 발견",
                defectQuantity = 1,
                status = ClaimStatus.SUBMITTED
            )
        )

        // store2의 클레임 (조회 대상 아님)
        claimRepository.save(
            Claim(
                user = testUser1,
                store = testStore2,
                storeName = testStore2.storeName,
                productCode = "PROD003",
                productName = "신라면",
                dateType = ClaimDateType.EXPIRY_DATE,
                date = LocalDate.of(2026, 12, 31),
                category = testCategory1,
                subcategory = testSubcategory1,
                defectDescription = "변질",
                defectQuantity = 1,
                status = ClaimStatus.SUBMITTED
            )
        )

        // When
        val claims = claimRepository.findByStoreIdOrderByCreatedAtDesc(testStore1.id)

        // Then
        assertThat(claims).hasSize(2)
        assertThat(claims[0].id).isEqualTo(claim2.id) // 최신순
        assertThat(claims[1].id).isEqualTo(claim1.id)
    }

    @Test
    @DisplayName("활성 상태인 클레임 종류1 목록을 sortOrder 순으로 조회할 수 있다")
    fun findActiveClaimCategories() {
        // When
        val categories = claimCategoryRepository.findByIsActiveTrueOrderBySortOrderAsc()

        // Then
        assertThat(categories).hasSize(1) // testCategory2는 비활성
        assertThat(categories[0].name).isEqualTo("이물")
    }

    @Test
    @DisplayName("특정 종류1에 속하는 활성 종류2 목록을 sortOrder 순으로 조회할 수 있다")
    fun findActiveClaimSubcategoriesByCategory() {
        // When
        val subcategories = claimSubcategoryRepository
            .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(testCategory1.id)

        // Then
        assertThat(subcategories).hasSize(2)
        assertThat(subcategories[0].name).isEqualTo("벌레")
        assertThat(subcategories[1].name).isEqualTo("금속")
    }

    @Test
    @DisplayName("활성 상태인 구매 방법 목록을 sortOrder 순으로 조회할 수 있다")
    fun findActivePurchaseMethods() {
        // When
        val methods = claimPurchaseMethodRepository.findByIsActiveTrueOrderBySortOrderAsc()

        // Then
        assertThat(methods).hasSize(1) // PM02는 비활성
        assertThat(methods[0].code).isEqualTo("PM01")
        assertThat(methods[0].name).isEqualTo("대형마트")
    }

    @Test
    @DisplayName("활성 상태인 요청사항 목록을 sortOrder 순으로 조회할 수 있다")
    fun findActiveRequestTypes() {
        // When
        val types = claimRequestTypeRepository.findByIsActiveTrueOrderBySortOrderAsc()

        // Then
        assertThat(types).hasSize(2)
        assertThat(types[0].code).isEqualTo("RT01")
        assertThat(types[0].name).isEqualTo("교환")
        assertThat(types[1].code).isEqualTo("RT02")
        assertThat(types[1].name).isEqualTo("환불")
    }
}
