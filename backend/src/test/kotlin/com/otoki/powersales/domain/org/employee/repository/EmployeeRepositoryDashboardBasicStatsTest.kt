package com.otoki.powersales.domain.org.employee.repository

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
import org.springframework.test.context.ActiveProfiles

/**
 * 대시보드 기본현황 projection 조회([EmployeeRepository.findDashboardBasicStatsProjection]) 통합 테스트.
 *
 * 퇴직자(status='퇴직') 는 재직 현황 모수에서 제외하되, status=NULL 사원은 유지하는지, 그리고
 * 지점 스코프(costCenterCode IN) / 전사(null·empty) 분기가 올바른지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeRepositoryDashboardBasicStatsTest {

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        employeeRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("전사 조회 - 퇴직자는 제외하고 재직/휴직/status=null 사원은 포함한다")
    fun excludesResignedIncludesNullStatus() {
        persist("EMP_ACTIVE", status = "재직", costCenterCode = "C001")
        persist("EMP_LEAVE", status = "휴직", costCenterCode = "C001")
        persist("EMP_RESIGNED", status = "퇴직", costCenterCode = "C001")
        persist("EMP_NULL", status = null, costCenterCode = "C001")

        val result = employeeRepository.findDashboardBasicStatsProjection(null)

        assertThat(result.map { it.status })
            .containsExactlyInAnyOrder("재직", "휴직", null)
        assertThat(result.map { it.status }).doesNotContain("퇴직")
    }

    @Test
    @DisplayName("지점 스코프 조회 - 지정 costCenterCode 사원만, 그 안에서도 퇴직자는 제외한다")
    fun branchScopeExcludesResigned() {
        persist("EMP_C001_ACTIVE", status = "재직", costCenterCode = "C001")
        persist("EMP_C001_RESIGNED", status = "퇴직", costCenterCode = "C001")
        persist("EMP_C002_ACTIVE", status = "재직", costCenterCode = "C002")

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("C001"))

        assertThat(result.map { it.status }).containsExactly("재직")
    }

    @Test
    @DisplayName("빈 지점 목록은 전사 조회로 동작한다 (퇴직자 제외)")
    fun emptyCodesFallsBackToOrgWide() {
        persist("EMP_C001", status = "재직", costCenterCode = "C001")
        persist("EMP_C002", status = "휴직", costCenterCode = "C002")
        persist("EMP_RESIGNED", status = "퇴직", costCenterCode = "C003")

        val result = employeeRepository.findDashboardBasicStatsProjection(emptyList())

        assertThat(result.map { it.status }).containsExactlyInAnyOrder("재직", "휴직")
    }

    @Test
    @DisplayName("여사원만 집계 - 조장/지점장/role=null 은 제외한다")
    fun onlyWomenRole() {
        persist("EMP_WOMAN", status = "재직", costCenterCode = "C001", role = "여사원")
        persist("EMP_LEADER", status = "재직", costCenterCode = "C001", role = "조장")
        persist("EMP_MANAGER", status = "재직", costCenterCode = "C001", role = "지점장")
        persist("EMP_NULL_ROLE", status = "재직", costCenterCode = "C001", role = null)

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("C001"))

        assertThat(result).hasSize(1)
    }

    private fun persist(
        employeeCode: String,
        status: String?,
        costCenterCode: String?,
        role: String? = "여사원",
    ) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = employeeCode,
                orgName = "부산1지점",
                status = status,
                costCenterCode = costCenterCode,
                role = role,
            )
        )
        testEntityManager.flush()
    }
}
