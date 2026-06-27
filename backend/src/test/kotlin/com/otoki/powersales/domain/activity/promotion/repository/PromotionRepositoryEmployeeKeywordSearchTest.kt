package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.querydsl.core.types.dsl.Expressions
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles

/**
 * 행사마스터 목록의 행사사원(employeeKeyword) 조회조건 — searchForAdmin EXISTS 필터 통합 테스트.
 *
 * "해당 행사에 (사번/성명 like 매칭되는) 행사사원이 1명이라도 배정된 행사만" 통과시키는지,
 * 그리고 1:N(행사:행사사원) 관계에서 EXISTS 가 promotion row 를 중복(fan-out) 시키지 않아
 * 페이징/count(totalElements) 정합이 유지되는지 실DB(H2)로 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("PromotionRepository 행사사원(employeeKeyword) 조회조건 테스트")
class PromotionRepositoryEmployeeKeywordSearchTest {

    @Autowired
    private lateinit var promotionRepository: PromotionRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    // 항상 참 — sharing rule 가시 범위와 무관하게 검색 필터 단독 동작만 검증.
    private val allowAll = Expressions.asBoolean(true).isTrue
    private val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())

    private var seq = 0

    @BeforeEach
    fun setUp() {
        testEntityManager.clear()
    }

    private fun persistPromotion(): Promotion =
        testEntityManager.persistAndFlush(
            Promotion(
                promotionNumber = "PM-${seq++}",
                startDate = LocalDate.of(2026, 6, 1),
                endDate = LocalDate.of(2026, 6, 30),
                costCenterCode = "5815",
                isDeleted = false,
            ),
        )

    private fun persistEmployee(code: String?, name: String): Employee =
        testEntityManager.persistAndFlush(Employee(employeeCode = code, name = name))

    private fun assignEmployee(
        promotionId: Long,
        employeeId: Long,
        isDeleted: Boolean? = null,
    ): PromotionEmployee =
        testEntityManager.persistAndFlush(
            PromotionEmployee(
                promotionId = promotionId,
                employeeId = employeeId,
                scheduleDate = LocalDate.of(2026, 6, 12),
                isDeleted = isDeleted,
            ),
        )

    private fun search(employeeKeyword: String?) =
        promotionRepository.searchForAdmin(
            policyPredicate = allowAll,
            keyword = null,
            promotionType = null,
            startDate = null,
            endDate = null,
            accountName = null,
            accountNumber = null,
            category1 = null,
            primaryProduct = null,
            employeeKeyword = employeeKeyword,
            ownerOnly = false,
            currentUserId = null,
            pageable = pageable,
        )

    @Test
    @DisplayName("성명 like — 매칭 사원이 배정된 행사만 반환")
    fun matchesByEmployeeName() {
        val matched = persistPromotion()
        val other = persistPromotion()
        val hong = persistEmployee("E001", "홍길동")
        val kim = persistEmployee("E002", "김철수")
        assignEmployee(matched.id, hong.id)
        assignEmployee(other.id, kim.id)

        val result = search("홍길")

        assertThat(result.content).extracting<Long> { it.id }.containsExactly(matched.id)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    @DisplayName("사번 like — 매칭 사원이 배정된 행사만 반환")
    fun matchesByEmployeeCode() {
        val matched = persistPromotion()
        val other = persistPromotion()
        assignEmployee(matched.id, persistEmployee("ABC123", "홍길동").id)
        assignEmployee(other.id, persistEmployee("XYZ999", "김철수").id)

        val result = search("abc")

        assertThat(result.content).extracting<Long> { it.id }.containsExactly(matched.id)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    @DisplayName("EXISTS — 한 행사에 매칭 사원 여러 명이어도 행사 row 는 1건 (fan-out 없음, count 정합)")
    fun noFanOutWhenMultipleMatches() {
        val promotion = persistPromotion()
        // 같은 행사에 "홍" 으로 매칭되는 행사사원 3명 배정
        assignEmployee(promotion.id, persistEmployee("E001", "홍길동").id)
        assignEmployee(promotion.id, persistEmployee("E002", "홍판서").id)
        assignEmployee(promotion.id, persistEmployee("E003", "홍영희").id)

        val result = search("홍")

        // JOIN 이었다면 3건으로 중복됐을 것 — EXISTS 라 1건 + totalElements=1
        assertThat(result.content).hasSize(1)
        assertThat(result.totalElements).isEqualTo(1)
    }

    @Test
    @DisplayName("soft-delete 된 행사사원만 매칭되면 그 행사는 제외")
    fun excludesWhenOnlyMatchIsSoftDeleted() {
        val promotion = persistPromotion()
        assignEmployee(promotion.id, persistEmployee("E001", "홍길동").id, isDeleted = true)

        val result = search("홍길동")

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @Test
    @DisplayName("employeeKeyword=null — 행사사원 필터 미적용 (전체 반환)")
    fun nullKeywordReturnsAll() {
        val p1 = persistPromotion()
        val p2 = persistPromotion()
        assignEmployee(p1.id, persistEmployee("E001", "홍길동").id)

        val result = search(null)

        assertThat(result.content).extracting<Long> { it.id }.containsExactlyInAnyOrder(p1.id, p2.id)
        assertThat(result.totalElements).isEqualTo(2)
    }
}
