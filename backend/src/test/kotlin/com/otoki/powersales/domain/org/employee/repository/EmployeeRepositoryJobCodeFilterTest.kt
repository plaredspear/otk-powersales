package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.FemaleStaffJobCode
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
 * 여사원 현황 목록의 직무(판촉직/OSC직) 필터 통합 테스트.
 *
 * 판정 축은 [Employee.jobCode] 로, 대시보드 "판촉직/OSC직 인원현황" 도넛과 동일하다
 * (`AdminDashboardService.buildBasicStats`). 'OSC직' 선택 시 구 명칭 '레이디직'(SAP A053,
 * 2024-01-02 개명 이전 적재분) 을 함께 조회해야 두 화면의 인원 수가 일치한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeRepositoryJobCodeFilterTest {

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        employeeRepository.deleteAll()
        testEntityManager.clear()

        persist("EMP_PROMOTION", jobCode = "판촉직")
        persist("EMP_OSC", jobCode = "OSC직")
        persist("EMP_LADY", jobCode = "레이디직")
        persist("EMP_SALES", jobCode = "영업직")
        persist("EMP_NULL", jobCode = null)
    }

    @Test
    @DisplayName("OSC직 필터 - 구 명칭 '레이디직' 사원도 함께 조회한다")
    fun oscFilterIncludesLady() {
        val result = findWithJobCodes(FemaleStaffJobCode.OSC_CODES)

        assertThat(result.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_OSC", "EMP_LADY")
    }

    @Test
    @DisplayName("판촉직 필터 - 판촉직 사원만 조회한다")
    fun promotionFilter() {
        val result = findWithJobCodes(setOf(FemaleStaffJobCode.PROMOTION.code))

        assertThat(result.content.map { it.employeeCode }).containsExactly("EMP_PROMOTION")
    }

    @Test
    @DisplayName("직무 필터 미적용(null) - 직무 무관 전건 조회")
    fun noFilterReturnsAll() {
        val result = findWithJobCodes(null)

        assertThat(result.content).hasSize(5)
    }

    @Test
    @DisplayName("빈 집합은 필터 미적용으로 동작한다 (빈 IN 절로 전건 배제되지 않음)")
    fun emptySetIsTreatedAsNoFilter() {
        val result = findWithJobCodes(emptySet())

        assertThat(result.content).hasSize(5)
    }

    @Test
    @DisplayName("판촉직 + OSC직 합계는 여사원 직무 전체와 일치한다 (대시보드 도넛 모수 정합)")
    fun promotionPlusOscEqualsAllFemaleStaff() {
        val promotion = findWithJobCodes(setOf(FemaleStaffJobCode.PROMOTION.code)).totalElements
        val osc = findWithJobCodes(FemaleStaffJobCode.OSC_CODES).totalElements
        val all = findWithJobCodes(FemaleStaffJobCode.ALL_CODES).totalElements

        assertThat(promotion + osc).isEqualTo(all)
    }

    private fun findWithJobCodes(jobCodes: Set<String>?) =
        employeeRepository.findEmployees(
            status = null,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            pageable = PageRequest.of(0, 20),
            jobCodes = jobCodes,
        )

    private fun persist(employeeCode: String, jobCode: String?) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = employeeCode,
                orgName = "부산1지점",
                status = "재직",
                costCenterCode = "C001",
                role = "여사원",
            ).apply { this.jobCode = jobCode }
        )
        testEntityManager.flush()
    }
}
