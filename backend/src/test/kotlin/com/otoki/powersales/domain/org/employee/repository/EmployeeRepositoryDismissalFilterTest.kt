package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.DismissalPolicy
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
import java.time.LocalDate

/**
 * 여사원 현황의 "발령명 '면직' = 퇴직" 처리 테스트 ([DismissalPolicy]).
 *
 * 면직 발령을 받고도 [Employee.status] 가 '재직' 으로 남은 사원이 운영 데이터에 있어,
 * 퇴직 조회에서 빠지고 재직 조회에 섞여 나오던 문제를 조회 시점에 보정한다.
 * `findEmployees` 는 전체 사원 관리/lookup 화면도 공유하므로, 플래그 미지정(기본 false) 시
 * SAP 원본 상태 그대로 동작하는지도 함께 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeRepositoryDismissalFilterTest {

    @Autowired
    private lateinit var employeeRepository: EmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        employeeRepository.deleteAll()
        testEntityManager.clear()

        // 상태 갱신이 늦어 '재직' 으로 남은 면직자 — 본 정책의 대상.
        persist("EMP_ACTIVE_DISMISSED", status = "재직", ordDetailNode = "면직")
        // 상태까지 정상 반영된 면직자.
        persist("EMP_RESIGNED_DISMISSED", status = "퇴직", ordDetailNode = "면직")
        // 면직이 아닌 일반 사원들 (발령명 null 행 포함 — SQL 3값 논리 회귀 방어).
        persist("EMP_ACTIVE", status = "재직", ordDetailNode = "조직개편")
        persist("EMP_ACTIVE_NO_ORD", status = "재직", ordDetailNode = null)
        persist("EMP_ON_LEAVE", status = "휴직", ordDetailNode = null)
        persist("EMP_RESIGNED", status = "퇴직", ordDetailNode = null)
    }

    @Test
    @DisplayName("퇴직 조회 - 상태가 '재직' 이어도 발령명이 면직이면 포함된다")
    fun resignedFilterIncludesDismissed() {
        val result = findWithStatus("퇴직", treatDismissalAsResigned = true)

        assertThat(result.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_RESIGNED", "EMP_RESIGNED_DISMISSED", "EMP_ACTIVE_DISMISSED")
    }

    @Test
    @DisplayName("재직 조회 - 발령명이 면직인 사원은 제외된다")
    fun activeFilterExcludesDismissed() {
        val result = findWithStatus("재직", treatDismissalAsResigned = true)

        // 발령명이 null 인 재직자가 함께 남아야 한다 (`<> '면직'` 만 쓰면 NULL 행이 통째로 탈락).
        assertThat(result.content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_ACTIVE", "EMP_ACTIVE_NO_ORD")
    }

    @Test
    @DisplayName("휴직 조회 - 퇴직과 마찬가지로 면직자는 섞이지 않는다")
    fun onLeaveFilterExcludesDismissed() {
        persist("EMP_ON_LEAVE_DISMISSED", status = "휴직", ordDetailNode = "면직")

        val result = findWithStatus("휴직", treatDismissalAsResigned = true)

        assertThat(result.content.map { it.employeeCode }).containsExactly("EMP_ON_LEAVE")
    }

    @Test
    @DisplayName("상태 미선택(전체) - 면직 여부와 무관하게 전건 조회")
    fun noStatusFilterReturnsAll() {
        val result = employeeRepository.findEmployees(
            status = null,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            pageable = PageRequest.of(0, 20),
            treatDismissalAsResigned = true,
        )

        assertThat(result.content).hasSize(6)
    }

    @Test
    @DisplayName("플래그 미적용(기본) - SAP 원본 상태 그대로 조회 (공유 화면 회귀 방어)")
    fun withoutFlagUsesRawStatus() {
        assertThat(findWithStatus("재직", treatDismissalAsResigned = false).content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_ACTIVE", "EMP_ACTIVE_NO_ORD", "EMP_ACTIVE_DISMISSED")
        assertThat(findWithStatus("퇴직", treatDismissalAsResigned = false).content.map { it.employeeCode })
            .containsExactlyInAnyOrder("EMP_RESIGNED", "EMP_RESIGNED_DISMISSED")
    }

    @Test
    @DisplayName("상태 표시 - 면직자는 '퇴직(면직)', 그 외는 원본 상태")
    fun displayStatusIsDerivedForDismissed() {
        val today = LocalDate.of(2026, 7, 29)
        val employees = findWithStatus("퇴직", treatDismissalAsResigned = true).content
            .associateBy { it.employeeCode }

        val items = employees.mapValues { (_, emp) ->
            EmployeeListItem.from(emp, today, treatDismissalAsResigned = true).status
        }
        assertThat(items["EMP_ACTIVE_DISMISSED"]).isEqualTo(DismissalPolicy.DISPLAY_STATUS)
        assertThat(items["EMP_RESIGNED_DISMISSED"]).isEqualTo(DismissalPolicy.DISPLAY_STATUS)
        assertThat(items["EMP_RESIGNED"]).isEqualTo("퇴직")

        // 플래그가 없는 화면(전체 사원 관리 등)은 원본 상태를 그대로 노출한다.
        val raw = employees.getValue("EMP_ACTIVE_DISMISSED")
        assertThat(EmployeeListItem.from(raw, today).status).isEqualTo("재직")
    }

    private fun findWithStatus(status: String, treatDismissalAsResigned: Boolean) =
        employeeRepository.findEmployees(
            status = status,
            branchCodes = null,
            keyword = null,
            role = null,
            roles = null,
            pageable = PageRequest.of(0, 20),
            treatDismissalAsResigned = treatDismissalAsResigned,
        )

    private fun persist(employeeCode: String, status: String, ordDetailNode: String?) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = employeeCode,
                orgName = "부산1지점",
                status = status,
                costCenterCode = "C001",
                role = "여사원",
            ).apply { this.ordDetailNode = ordDetailNode }
        )
        testEntityManager.flush()
    }
}
