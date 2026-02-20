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
@DisplayName("InspectionRepository 테스트")
class InspectionRepositoryTest {

    @Autowired
    private lateinit var inspectionRepository: InspectionRepository

    @Autowired
    private lateinit var inspectionThemeRepository: InspectionThemeRepository

    @Autowired
    private lateinit var inspectionFieldTypeRepository: InspectionFieldTypeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser1: User
    private lateinit var testUser2: User
    private lateinit var testStore1: Store
    private lateinit var testStore2: Store
    private lateinit var testTheme1: InspectionTheme
    private lateinit var testFieldType1: InspectionFieldType
    private lateinit var testFieldType2: InspectionFieldType

    @BeforeEach
    fun setUp() {
        inspectionRepository.deleteAll()
        inspectionThemeRepository.deleteAll()
        inspectionFieldTypeRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        testUser1 = testEntityManager.persistAndFlush(User(
            employeeId = "10000001",
            password = "encoded",
            name = "홍길동",
            orgName = "서울지점"
        ))

        testUser2 = testEntityManager.persistAndFlush(User(
            employeeId = "10000002",
            password = "encoded",
            name = "김영희",
            orgName = "부산지점"
        ))

        // 테스트 거래처 생성
        testStore1 = testEntityManager.persistAndFlush(Store(
            storeCode = "ST001",
            storeName = "롯데마트 사상"
        ))

        testStore2 = testEntityManager.persistAndFlush(Store(
            storeCode = "ST002",
            storeName = "이마트 트레이더스"
        ))

        // 테스트 테마 생성
        testTheme1 = testEntityManager.persistAndFlush(InspectionTheme(
            name = "8월 테마",
            startDate = LocalDate.of(2020, 8, 1),
            endDate = LocalDate.of(2020, 8, 31),
            isActive = true
        ))

        // 테스트 현장 유형 생성
        testFieldType1 = testEntityManager.persistAndFlush(InspectionFieldType(
            code = "FT01",
            name = "본매대",
            sortOrder = 1,
            isActive = true
        ))

        testFieldType2 = testEntityManager.persistAndFlush(InspectionFieldType(
            code = "FT02",
            name = "시식",
            sortOrder = 2,
            isActive = true
        ))

        testEntityManager.persistAndFlush(InspectionFieldType(
            code = "FT99",
            name = "비활성 유형",
            sortOrder = 99,
            isActive = false
        ))

        // 테스트 점검 데이터 생성
        createInspection(
            user = testUser1,
            store = testStore1,
            category = InspectionCategory.OWN,
            date = LocalDate.of(2020, 8, 18),
            fieldTypeCode = "FT01",
            fieldTypeName = "본매대",
            productCode = "12345678",
            productName = "맛있는부대찌개라양념140G"
        )

        createInspection(
            user = testUser1,
            store = testStore1,
            category = InspectionCategory.COMPETITOR,
            date = LocalDate.of(2020, 8, 18),
            fieldTypeCode = "FT02",
            fieldTypeName = "시식",
            competitorName = "경쟁사1"
        )

        createInspection(
            user = testUser1,
            store = testStore2,
            category = InspectionCategory.OWN,
            date = LocalDate.of(2020, 8, 25),
            fieldTypeCode = "FT01",
            fieldTypeName = "본매대",
            productCode = "87654321",
            productName = "제품2"
        )

        createInspection(
            user = testUser2,
            store = testStore1,
            category = InspectionCategory.OWN,
            date = LocalDate.of(2020, 8, 20),
            fieldTypeCode = "FT01",
            fieldTypeName = "본매대",
            productCode = "11111111",
            productName = "제품3"
        )
    }

    @Test
    @DisplayName("사용자별 현장 점검 목록을 기간으로 조회한다")
    fun findByUserIdWithFilters_DateRange() {
        // Given: 2020-08-01 ~ 2020-08-31 기간, 필터 없음

        // When
        val result = inspectionRepository.findByUserIdWithFilters(
            userId = testUser1.id,
            fromDate = LocalDate.of(2020, 8, 1),
            toDate = LocalDate.of(2020, 8, 31),
            storeId = null,
            category = null
        )

        // Then
        assertThat(result).hasSize(3) // testUser1의 점검 3건
        assertThat(result[0].inspectionDate).isEqualTo(LocalDate.of(2020, 8, 25)) // 최신순
        assertThat(result[1].inspectionDate).isEqualTo(LocalDate.of(2020, 8, 18))
        assertThat(result[2].inspectionDate).isEqualTo(LocalDate.of(2020, 8, 18))
    }

    @Test
    @DisplayName("사용자별 현장 점검 목록을 거래처 필터로 조회한다")
    fun findByUserIdWithFilters_WithStore() {
        // Given: testStore1 필터 적용

        // When
        val result = inspectionRepository.findByUserIdWithFilters(
            userId = testUser1.id,
            fromDate = LocalDate.of(2020, 8, 1),
            toDate = LocalDate.of(2020, 8, 31),
            storeId = testStore1.id,
            category = null
        )

        // Then
        assertThat(result).hasSize(2) // testStore1의 점검 2건
        assertThat(result).allMatch { it.store.id == testStore1.id }
    }

    @Test
    @DisplayName("사용자별 현장 점검 목록을 분류 필터로 조회한다")
    fun findByUserIdWithFilters_WithCategory() {
        // Given: OWN 분류 필터 적용

        // When
        val result = inspectionRepository.findByUserIdWithFilters(
            userId = testUser1.id,
            fromDate = LocalDate.of(2020, 8, 1),
            toDate = LocalDate.of(2020, 8, 31),
            storeId = null,
            category = InspectionCategory.OWN
        )

        // Then
        assertThat(result).hasSize(2) // 자사 점검 2건
        assertThat(result).allMatch { it.category == InspectionCategory.OWN }
    }

    @Test
    @DisplayName("기간 밖의 데이터는 조회되지 않는다")
    fun findByUserIdWithFilters_OutOfRange() {
        // Given: 2020-09-01 ~ 2020-09-30 기간 (데이터 없음)

        // When
        val result = inspectionRepository.findByUserIdWithFilters(
            userId = testUser1.id,
            fromDate = LocalDate.of(2020, 9, 1),
            toDate = LocalDate.of(2020, 9, 30),
            storeId = null,
            category = null
        )

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("현장 점검 상세 조회 시 사진 목록이 함께 조회된다")
    fun findByIdWithPhotos() {
        // Given: 사진이 있는 점검 데이터 생성
        val inspection = createInspection(
            user = testUser1,
            store = testStore1,
            category = InspectionCategory.OWN,
            date = LocalDate.of(2020, 8, 19),
            fieldTypeCode = "FT01",
            fieldTypeName = "본매대",
            productCode = "99999999",
            productName = "테스트제품"
        )

        val photo1 = testEntityManager.persistAndFlush(InspectionPhoto(
            inspection = inspection,
            url = "https://storage.example.com/photo1.jpg",
            originalFileName = "photo1.jpg",
            fileSize = 1024000,
            contentType = "image/jpeg"
        ))

        testEntityManager.persistAndFlush(InspectionPhoto(
            inspection = inspection,
            url = "https://storage.example.com/photo2.jpg",
            originalFileName = "photo2.jpg",
            fileSize = 2048000,
            contentType = "image/jpeg"
        ))

        testEntityManager.clear()

        // When
        val result = inspectionRepository.findByIdWithPhotos(inspection.id)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.photos).hasSize(2)
        assertThat(result.photos[0].url).contains("photo")
    }

    @Test
    @DisplayName("특정 날짜 기준 활성 테마를 조회한다")
    fun findActiveThemesByDate() {
        // Given: 2020-08-15 기준 (테마 기간 내)
        testEntityManager.persistAndFlush(InspectionTheme(
            name = "7월 테마",
            startDate = LocalDate.of(2020, 7, 1),
            endDate = LocalDate.of(2020, 7, 31),
            isActive = true
        ))

        testEntityManager.persistAndFlush(InspectionTheme(
            name = "9월 테마",
            startDate = LocalDate.of(2020, 9, 1),
            endDate = LocalDate.of(2020, 9, 30),
            isActive = true
        ))

        testEntityManager.persistAndFlush(InspectionTheme(
            name = "비활성 테마",
            startDate = LocalDate.of(2020, 8, 1),
            endDate = LocalDate.of(2020, 8, 31),
            isActive = false
        ))

        // When
        val result = inspectionThemeRepository.findActiveThemesByDate(
            targetDate = LocalDate.of(2020, 8, 15)
        )

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("8월 테마")
    }

    @Test
    @DisplayName("활성 상태인 현장 유형 목록을 sortOrder 순으로 조회한다")
    fun findActiveFieldTypes() {
        // When
        val result = inspectionFieldTypeRepository.findActiveFieldTypes()

        // Then
        assertThat(result).hasSize(2) // 활성 유형 2개
        assertThat(result[0].code).isEqualTo("FT01") // sortOrder=1
        assertThat(result[1].code).isEqualTo("FT02") // sortOrder=2
    }

    private fun createInspection(
        user: User,
        store: Store,
        category: InspectionCategory,
        date: LocalDate,
        fieldTypeCode: String,
        fieldTypeName: String,
        productCode: String? = null,
        productName: String? = null,
        competitorName: String? = null
    ): Inspection {
        return testEntityManager.persistAndFlush(Inspection(
            user = user,
            store = store,
            theme = testTheme1,
            category = category,
            storeName = store.storeName,
            inspectionDate = date,
            fieldTypeCode = fieldTypeCode,
            fieldTypeName = fieldTypeName,
            productCode = productCode,
            productName = productName,
            competitorName = competitorName
        ))
    }
}
