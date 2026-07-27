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
 * 여사원 현황 목록의 직무(판촉직/OSC직) 필터 + 인원현황 모수(`femaleStaffHeadcountScope`) 통합 테스트.
 *
 * 판정 축은 [Employee.jobCode] 로, 대시보드 "판촉직/OSC직 인원현황" 도넛과 동일하다
 * (`AdminDashboardService.buildBasicStats`). 'OSC직' 선택 시 구 명칭 '레이디직'(SAP A053,
 * 2024-01-02 개명 이전 적재분) 을 함께 조회해야 두 화면의 인원 수가 일치한다.
 *
 * 인원현황 모수는 레거시 SF 홈 대시보드(조장) 리포트(`reports/X00/new_report_72Y`) 정합으로
 * 여사원 직무 3값 한정 + 테스트/시스템 계정 제외를 적용한다. `findEmployees` 는 전체 사원 관리 등
 * 다른 화면도 공유하므로, 플래그 미지정(기본 false) 시 기존 모수가 유지되는지도 함께 검증한다.
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

    @Test
    @DisplayName("인원현황 모수 적용 시 - 여사원 직무 3값만 남고 영업직/jobCode=null 은 빠진다")
    fun headcountScopeLimitsToFemaleStaffJobs() {
        val result = findWithHeadcountScope()

        assertThat(result.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_PROMOTION", "EMP_OSC", "EMP_LADY")
    }

    @Test
    @DisplayName("인원현황 모수 미적용(기본) 시 - 직무 무관 전건 조회 (공유 화면 회귀 방어)")
    fun withoutHeadcountScopeReturnsAll() {
        val result = findWithJobCodes(null)

        assertThat(result.content).hasSize(5)
    }

    @Test
    @DisplayName("인원현황 모수 적용 시 - 사원명에 테스트·관리자·파워세일즈 포함 사원은 빠진다")
    fun headcountScopeExcludesTestAccounts() {
        persist("EMP_TEST", jobCode = "판촉직", name = "테스트여사")
        persist("EMP_ADMIN", jobCode = "OSC직", name = "시스템관리자")
        persist("EMP_PWRS", jobCode = "판촉직", name = "파워세일즈운영")

        val result = findWithHeadcountScope()

        // 직무는 모수 안이지만 이름 규칙에 걸려 제외된다 — 추가 3명이 반영되지 않아야 한다.
        assertThat(result.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_PROMOTION", "EMP_OSC", "EMP_LADY")
    }

    @Test
    @DisplayName("인원현황 모수 + 직무 필터는 AND 로 겹친다 - OSC직 선택 시 레이디직 포함 2명")
    fun headcountScopeCombinedWithJobCodeFilter() {
        val result = employeeRepository.findEmployees(
            status = null,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            pageable = PageRequest.of(0, 20),
            jobCodes = FemaleStaffJobCode.OSC_CODES,
            femaleStaffHeadcountScope = true,
        )

        assertThat(result.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_OSC", "EMP_LADY")
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

    private fun findWithHeadcountScope() =
        employeeRepository.findEmployees(
            status = null,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            pageable = PageRequest.of(0, 20),
            femaleStaffHeadcountScope = true,
        )

    private fun persist(employeeCode: String, jobCode: String?, name: String = employeeCode) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = name,
                orgName = "부산1지점",
                status = "재직",
                costCenterCode = "C001",
                role = "여사원",
            ).apply { this.jobCode = jobCode }
        )
        testEntityManager.flush()
    }
}
