package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
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
import org.springframework.test.context.ActiveProfiles

/**
 * 전문행사조 "일반" 필터 통합 테스트.
 *
 * 신규 시스템은 미배정을 null 로 표현하지만, SF 레거시가 정규화 없이 적재한 '일반'·'해당없음'
 * 문자열 행이 DB 에 남아 있다 (converter 가 5개 정식 조가 아니면 null 로 변환하므로 화면에는
 * '일반'으로 표시됨). "일반" 필터가 IS NULL 뿐 아니라 이 문자열 행까지 함께 조회하는지 검증한다.
 *
 * '일반'·'해당없음' 문자열은 entity 저장 경로로는 만들 수 없어 (converter 가 null↔enum 만 매핑)
 * native UPDATE 로 컬럼에 직접 주입한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeRepositoryPromotionTeamFilterTest {

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        employeeRepository.deleteAll()
        testEntityManager.clear()
        dropPromotionTeamCheckConstraint()
    }

    /**
     * H2 ddl-auto 는 enum property 에 대해 정식 조 5개만 허용하는 CHECK 제약을 생성한다.
     * 운영 PostgreSQL 에는 이 제약이 없어 '일반'·'해당없음' 레거시 문자열이 실제로 적재돼 있으므로,
     * 그 상황을 재현하기 위해 테스트 DB 의 CHECK 제약을 제거한다.
     */
    private fun dropPromotionTeamCheckConstraint() {
        @Suppress("UNCHECKED_CAST")
        val constraintNames = testEntityManager.entityManager
            .createNativeQuery(
                """
                SELECT constraint_name FROM information_schema.check_constraints
                WHERE check_clause LIKE '%PROFESSIONAL_PROMOTION_TEAM%'
                """.trimIndent()
            )
            .resultList as List<String>
        constraintNames.forEach { name ->
            testEntityManager.entityManager
                .createNativeQuery("ALTER TABLE employee DROP CONSTRAINT IF EXISTS \"$name\"")
                .executeUpdate()
        }
    }

    @Test
    @DisplayName("전문행사조 '일반' 필터 - NULL / '일반' 문자열 / '해당없음' 문자열 행을 모두 조회한다")
    fun general_filter_matches_null_and_legacy_strings() {
        // NULL (신규 미배정)
        persist("EMP_NULL", promotionTeam = null)
        // '라면세일조' (정식 조) — 조회에서 제외되어야 함
        persist("EMP_RAMEN", promotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
        // '일반' / '해당없음' 문자열 (SF 레거시 미정규화) — native UPDATE 로 주입
        persist("EMP_GENERAL_STR", promotionTeam = null)
        persist("EMP_NONE_STR", promotionTeam = null)
        updatePromotionTeamRaw("EMP_GENERAL_STR", "일반")
        updatePromotionTeamRaw("EMP_NONE_STR", "해당없음")

        val page = employeeRepository.findEmployees(
            status = null,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            promotionTeam =null,
            promotionTeamGeneral = true,
            pageable = PageRequest.of(0, 20),
        )

        assertThat(page.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_NULL", "EMP_GENERAL_STR", "EMP_NONE_STR")
        assertThat(page.content.map { it.employeeCode }).doesNotContain("EMP_RAMEN")
    }

    @Test
    @DisplayName("전문행사조 특정 조 필터 - 등호 매칭으로 해당 조만 조회한다")
    fun specific_team_filter_matches_only_that_team() {
        persist("EMP_NULL", promotionTeam = null)
        persist("EMP_RAMEN", promotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
        persist("EMP_CURRY", promotionTeam = ProfessionalPromotionTeamType.CURRY_PROMOTION)
        persist("EMP_GENERAL_STR", promotionTeam = null)
        updatePromotionTeamRaw("EMP_GENERAL_STR", "일반")

        val page = employeeRepository.findEmployees(
            status = null,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            promotionTeam =ProfessionalPromotionTeamType.RAMEN_SALE,
            promotionTeamGeneral = false,
            pageable = PageRequest.of(0, 20),
        )

        assertThat(page.content.map { it.employeeCode }).containsExactly("EMP_RAMEN")
    }

    private fun persist(employeeCode: String, promotionTeam: ProfessionalPromotionTeamType?) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = employeeCode,
                orgName = "부산1지점",
                professionalPromotionTeam = promotionTeam,
            )
        )
        testEntityManager.flush()
    }

    /**
     * converter 를 우회해 컬럼에 레거시 문자열('일반'/'해당없음')을 직접 주입한다.
     * entity 저장 경로는 이 값들을 표현할 수 없으므로 native UPDATE 를 사용.
     */
    private fun updatePromotionTeamRaw(employeeCode: String, rawValue: String) {
        testEntityManager.entityManager
            .createNativeQuery(
                "UPDATE employee SET professional_promotion_team = :v WHERE employee_code = :code"
            )
            .setParameter("v", rawValue)
            .setParameter("code", employeeCode)
            .executeUpdate()
        testEntityManager.clear()
    }
}
