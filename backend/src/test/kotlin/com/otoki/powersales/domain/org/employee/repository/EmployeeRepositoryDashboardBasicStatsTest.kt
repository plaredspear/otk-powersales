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
 * 지점 스코프(**org_name IN** — 레거시 리포트 OrgName 그룹핑 정합) / 전사(null·empty) 분기가
 * 올바른지 검증한다. 지점 축이 `cost_center_code` 가 아니라 조직명임에 주의 — 같은 대시보드의
 * 투입현황과 여사원 현황 목록은 코드 축을 쓴다.
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
    @DisplayName("지점 스코프 조회 - 지정 조직명(org_name) 사원만, 그 안에서도 퇴직자는 제외한다")
    fun branchScopeExcludesResigned() {
        persist("EMP_B1_ACTIVE", status = "재직", costCenterCode = "C001", orgName = "부산1지점")
        persist("EMP_B1_RESIGNED", status = "퇴직", costCenterCode = "C001", orgName = "부산1지점")
        persist("EMP_B2_ACTIVE", status = "재직", costCenterCode = "C002", orgName = "부산2지점")

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("부산1지점"))

        assertThat(result.map { it.status }).containsExactly("재직")
    }

    @Test
    @DisplayName("지점 스코프는 조직명 기준 - 같은 조직명이면 costCenterCode 가 달라도(옛 코드 잔존) 포함된다")
    fun branchScopeMatchesByOrgNameNotCode() {
        // 조직 개편 후 발령 미수신으로 옛 코드(5459)가 남은 사원 — 조직명은 동일
        persist("EMP_CURRENT", status = "재직", costCenterCode = "5824", orgName = "강남2지점")
        persist("EMP_OLD_CODE", status = "재직", costCenterCode = "5459", orgName = "강남2지점")
        // 조직명이 다르면 코드가 스코프와 같아도 제외
        persist("EMP_OTHER_NAME", status = "재직", costCenterCode = "5824", orgName = "강남3지점")

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("강남2지점"))

        assertThat(result).hasSize(2)
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
    @DisplayName("여사원+조장 집계 - 지점장/role=null 은 제외한다 (레거시 리포트 AppAuthority IN (조장,여사원))")
    fun womenAndLeaderRoles() {
        persist("EMP_WOMAN", status = "재직", costCenterCode = "C001", role = "여사원")
        persist("EMP_LEADER", status = "재직", costCenterCode = "C001", role = "조장")
        persist("EMP_MANAGER", status = "재직", costCenterCode = "C001", role = "지점장")
        persist("EMP_NULL_ROLE", status = "재직", costCenterCode = "C001", role = null)

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("부산1지점"))

        // 조장은 여사원 조직 관리자라 인원현황에 함께 계상된다 (레거시 new_report_72Y 정합).
        assertThat(result).hasSize(2)
    }

    @Test
    @DisplayName("여사원 직무 3값만 집계 - 영업직/jobCode=null 은 모수에서 제외한다")
    fun onlyFemaleStaffJobCodes() {
        persist("EMP_PROMOTION", status = "재직", costCenterCode = "C001", jobCode = "판촉직")
        persist("EMP_OSC", status = "재직", costCenterCode = "C001", jobCode = "OSC직")
        persist("EMP_LADY", status = "재직", costCenterCode = "C001", jobCode = "레이디직")
        persist("EMP_SALES", status = "재직", costCenterCode = "C001", jobCode = "영업직")
        persist("EMP_NO_JOB", status = "재직", costCenterCode = "C001", jobCode = null)

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("부산1지점"))

        assertThat(result.map { it.jobCode })
            .containsExactlyInAnyOrder("판촉직", "OSC직", "레이디직")
    }

    @Test
    @DisplayName("테스트/시스템 계정 제외 - 사원명에 테스트·관리자·파워세일즈 포함 시 모수에서 뺀다")
    fun excludesTestAccountNames() {
        persist("EMP_REAL", status = "재직", costCenterCode = "C001", name = "김여사")
        persist("EMP_TEST", status = "재직", costCenterCode = "C001", name = "테스트계정")
        persist("EMP_ADMIN", status = "재직", costCenterCode = "C001", name = "시스템관리자")
        persist("EMP_PWRS", status = "재직", costCenterCode = "C001", name = "파워세일즈운영")

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("부산1지점"))

        assertThat(result).hasSize(1)
    }

    @Test
    @DisplayName("삭제된 사원(is_deleted=true) 은 모수에서 제외한다 - 여사원 현황 목록과 동일 축")
    fun excludesDeleted() {
        persist("EMP_LIVE", status = "재직", costCenterCode = "C001")
        persist("EMP_NULL_FLAG", status = "재직", costCenterCode = "C001", isDeleted = null)
        persist("EMP_DELETED", status = "재직", costCenterCode = "C001", isDeleted = true)

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("부산1지점"))

        // is_deleted 가 false / null 인 사원만 남는다 (레거시 적재분은 flag 가 null).
        assertThat(result).hasSize(2)
    }

    @Test
    @DisplayName("직무코드는 그대로 내려준다 - 레이디직(구 OSC) 원본값 보존")
    fun exposesRawJobCode() {
        persist("EMP_PROMOTION", status = "재직", costCenterCode = "C001", jobCode = "판촉직")
        persist("EMP_OSC", status = "재직", costCenterCode = "C001", jobCode = "OSC직")
        persist("EMP_LADY", status = "재직", costCenterCode = "C001", jobCode = "레이디직")

        val result = employeeRepository.findDashboardBasicStatsProjection(listOf("부산1지점"))

        // 레이디직 → OSC직 합산은 서비스 레이어(AdminDashboardService.buildBasicStats) 책임이므로
        // projection 은 원본값을 그대로 노출한다.
        assertThat(result.map { it.jobCode })
            .containsExactlyInAnyOrder("판촉직", "OSC직", "레이디직")
    }

    private fun persist(
        employeeCode: String,
        status: String?,
        costCenterCode: String?,
        role: String? = "여사원",
        // 레거시 리포트 정합으로 모수가 여사원 직무 3값에 한정되므로 기본값을 판촉직으로 둔다
        // (직무를 명시하지 않는 테스트는 모수 안에 있어야 status/지점 축 검증이 성립).
        jobCode: String? = "판촉직",
        isDeleted: Boolean? = false,
        name: String = employeeCode,
        // 기본현황 지점 필터 축 — 코드가 아니라 **조직명** 이다 (레거시 리포트 OrgName 그룹핑 정합).
        orgName: String? = "부산1지점",
    ) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = name,
                orgName = orgName,
                status = status,
                costCenterCode = costCenterCode,
                role = role,
                isDeleted = isDeleted,
            ).apply { this.jobCode = jobCode }
        )
        testEntityManager.flush()
    }
}
