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
import java.time.LocalDateTime

/**
 * 앱 미설치 추정 여사원 조회([EmployeeRepository.findAppUninstalledFemaleStaff]) +
 * 안내 대상 모수 집계([EmployeeRepository.countAppLoginTargetFemaleStaff]) 통합 테스트.
 *
 * 두 축을 함께 검증한다 — ① 앱 사용 흔적(app_version_seen_at / fcm_token) 판정,
 * ② 여사원 현황과 동일한 인원 모수(재직 + 여사원/조장 + 판촉직·OSC직 + 테스트 계정 제외) 에
 * 앱 로그인 가능 조건(app_login_active, 사번 보유) 을 더한 필터.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeRepositoryAppInstallTest {

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
    @DisplayName("앱 사용 흔적 판정 - 마지막 실행 기록도 FCM 토큰도 없는 사원만 미설치로 잡는다")
    fun onlyEmployeesWithoutAnyAppTrace() {
        persist("EMP_NONE")
        // 앱을 실행한 적이 있으면(로그인/토큰 리프레시 시 갱신) 설치자로 본다.
        persist("EMP_SEEN", appVersionSeenAt = LocalDateTime.of(2026, 7, 1, 9, 0))
        // 토큰이 남아 있으면 앱이 등록된 흔적이라 미설치로 단정하지 않는다.
        persist("EMP_TOKEN", fcmToken = "token-1")
        persist("EMP_BOTH", appVersionSeenAt = LocalDateTime.of(2026, 7, 1, 9, 0), fcmToken = "token-2")

        val result = employeeRepository.findAppUninstalledFemaleStaff()

        assertThat(result.map { it.employeeCode }).containsExactly("EMP_NONE")
    }

    @Test
    @DisplayName("앱 로그인 비활성/사번 미보유 사원은 제외한다 - 안내해도 로그인할 수 없는 사원")
    fun excludesEmployeesWhoCannotLogIn() {
        persist("EMP_ACTIVE")
        persist("EMP_INACTIVE", appLoginActive = false)
        persist("EMP_NULL_FLAG", appLoginActive = null)
        // 사번 없는 위탁 진열사원 — employee_info 자체가 만들어지지 않는다.
        persist(null)

        val result = employeeRepository.findAppUninstalledFemaleStaff()

        assertThat(result.map { it.employeeCode }).containsExactly("EMP_ACTIVE")
    }

    @Test
    @DisplayName("재직자만 집계 - 퇴직/휴직 및 발령명 '면직' 사원은 제외한다")
    fun onlyActiveEmployees() {
        persist("EMP_ACTIVE", status = "재직")
        persist("EMP_LEAVE", status = "휴직")
        persist("EMP_RESIGNED", status = "퇴직")
        persist("EMP_NULL_STATUS", status = null)
        // 면직 발령을 받고도 status 가 '재직' 으로 남은 사원 — 여사원 현황 재직 조회와 동일 판정으로 제외.
        persist("EMP_DISMISSED", status = "재직", ordDetailNode = "면직")

        val result = employeeRepository.findAppUninstalledFemaleStaff()

        assertThat(result.map { it.employeeCode }).containsExactly("EMP_ACTIVE")
    }

    @Test
    @DisplayName("여사원 현황과 동일 모수 - 조장은 포함하고 지점장/영업직/테스트 계정/삭제분은 제외한다")
    fun femaleStaffHeadcountScope() {
        persist("EMP_WOMAN", role = "여사원")
        persist("EMP_LEADER", role = "조장")
        persist("EMP_MANAGER", role = "지점장")
        persist("EMP_SALES_JOB", jobCode = "영업직")
        persist("EMP_NO_JOB", jobCode = null)
        persist("EMP_TEST", name = "테스트계정")
        persist("EMP_DELETED", isDeleted = true)

        val result = employeeRepository.findAppUninstalledFemaleStaff()

        assertThat(result.map { it.employeeCode }).containsExactlyInAnyOrder("EMP_WOMAN", "EMP_LEADER")
    }

    @Test
    @DisplayName("정렬 - 지점명 → 사번 오름차순 (엑셀에서 지점별로 묶여 읽히도록)")
    fun sortedByBranchThenEmployeeCode() {
        persist("EMP_002", orgName = "부산1지점")
        persist("EMP_001", orgName = "부산1지점")
        persist("EMP_003", orgName = "강남1지점")

        val result = employeeRepository.findAppUninstalledFemaleStaff()

        assertThat(result.map { it.employeeCode }).containsExactly("EMP_003", "EMP_001", "EMP_002")
    }

    @Test
    @DisplayName("안내 대상 모수 - 앱 사용 흔적과 무관하게 로그인 가능한 재직 여사원 전원을 센다")
    fun countTargetIgnoresAppTrace() {
        persist("EMP_NONE")
        persist("EMP_SEEN", appVersionSeenAt = LocalDateTime.of(2026, 7, 1, 9, 0))
        persist("EMP_TOKEN", fcmToken = "token-1")
        // 모수 밖 — 로그인 불가 / 재직 아님 / 여사원 직무 아님
        persist("EMP_INACTIVE", appLoginActive = false)
        persist("EMP_RESIGNED", status = "퇴직")
        persist("EMP_SALES_JOB", jobCode = "영업직")

        assertThat(employeeRepository.countAppLoginTargetFemaleStaff()).isEqualTo(3L)
        assertThat(employeeRepository.findAppUninstalledFemaleStaff()).hasSize(1)
    }

    private fun persist(
        employeeCode: String?,
        status: String? = "재직",
        role: String? = "여사원",
        jobCode: String? = "판촉직",
        appLoginActive: Boolean? = true,
        isDeleted: Boolean? = false,
        name: String = employeeCode ?: "무사번사원",
        orgName: String? = "부산1지점",
        ordDetailNode: String? = null,
        // 마지막 앱 실행 시각 — 로그인/토큰 리프레시 때 클라이언트가 보고한 앱 버전과 함께 갱신된다.
        appVersionSeenAt: LocalDateTime? = null,
        fcmToken: String? = null,
    ) {
        testEntityManager.persist(
            Employee(
                employeeCode = employeeCode,
                password = "encodedPassword",
                name = name,
                orgName = orgName,
                status = status,
                costCenterCode = "C001",
                role = role,
                appLoginActive = appLoginActive,
                isDeleted = isDeleted,
                fcmToken = fcmToken,
            ).apply {
                this.jobCode = jobCode
                this.ordDetailNode = ordDetailNode
                if (appVersionSeenAt != null) {
                    recordAppVersion("1.0.0", 1L, "ANDROID", appVersionSeenAt)
                }
            }
        )
        testEntityManager.flush()
    }
}
