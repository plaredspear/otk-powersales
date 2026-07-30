package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.admin.exception.SapOriginEmployeeNotEditableException
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeRoleUpdateRequest
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeUpdateRequest
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeUpdateService
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminEmployeeUpdateService 테스트 (UC-07)")
class AdminEmployeeUpdateServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val userRepository: UserRepository = mockk(relaxed = true)

    private val service = AdminEmployeeUpdateService(
        employeeRepository,
        userRepository,
    )

    @Test
    @DisplayName("origin=MANUAL 사원 -> 정보 수정 성공 + 응답에 새 값 반영")
    fun update_manualOrigin_success() {
        val existing = Employee(
            id = 10L,
            employeeCode = "100100",
            name = "수정전",
        ).apply {
            origin = EmployeeOrigin.MANUAL
            jikchak = "기존직책"
            role = AppAuthority.WOMAN
        }
        every { employeeRepository.findWithEmployeeInfoById(10L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val request = AdminEmployeeUpdateRequest(
            jikchak = "새직책",
            role = AppAuthority.LEADER,
            orgName = "신규조직",
        )

        val response = service.update(10L, request)

        assertThat(response.jikchak).isEqualTo("새직책")
        assertThat(response.role).isEqualTo(AppAuthority.LEADER)
        assertThat(response.orgName).isEqualTo("신규조직")
    }

    @Test
    @DisplayName("origin=SAP 사원 -> SapOriginEmployeeNotEditableException")
    fun update_sapOrigin_blocked() {
        val sapEmployee = Employee(id = 11L, employeeCode = "100200", name = "SAP사원")
            .apply { origin = EmployeeOrigin.SAP }
        every { employeeRepository.findWithEmployeeInfoById(11L) } returns sapEmployee

        assertThatThrownBy {
            service.update(11L, AdminEmployeeUpdateRequest(jikchak = "변경시도"))
        }.isInstanceOf(SapOriginEmployeeNotEditableException::class.java)
            .hasMessageContaining("100200")
    }

    @Test
    @DisplayName("존재하지 않는 사원 -> EmployeeNotFoundException")
    fun update_notFound() {
        every { employeeRepository.findWithEmployeeInfoById(999L) } returns null

        assertThatThrownBy {
            service.update(999L, AdminEmployeeUpdateRequest())
        }.isInstanceOf(EmployeeNotFoundException::class.java)
    }

    @Test
    @DisplayName("Trigger 부수 효과 - 잠금 ON -> 앱 로그인 자동 OFF")
    fun update_lockingFlag_disablesAppLogin() {
        val existing = Employee(id = 12L, employeeCode = "100300", name = "잠금테스트")
            .apply {
                origin = EmployeeOrigin.MANUAL
                appLoginActive = true
                lockingFlag = false
            }
        every { employeeRepository.findWithEmployeeInfoById(12L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val response = service.update(
            12L,
            AdminEmployeeUpdateRequest(lockingFlag = true, appLoginActive = true)
        )

        assertThat(response.lockingFlag).isTrue()
        assertThat(response.appLoginActive).isFalse() // Trigger 자동 비활성화
    }

    @Test
    @DisplayName("Trigger 부수 효과 - 판촉직 여사원 잠금 시도 -> 보호 복원 (잠금 해제 + 앱 로그인 유지)")
    fun update_protectedFieldStaff_lockingRestored() {
        val existing = Employee(id = 14L, employeeCode = "100500", name = "판촉여사원")
            .apply {
                origin = EmployeeOrigin.MANUAL
                jobCode = "판촉직"
                status = "재직"
                role = AppAuthority.WOMAN
                appLoginActive = true
                lockingFlag = false
            }
        every { employeeRepository.findWithEmployeeInfoById(14L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val response = service.update(
            14L,
            AdminEmployeeUpdateRequest(lockingFlag = true, appLoginActive = false)
        )

        // SF EmployeeTriggerHandler.lockingFlagException 동등 — 현장 여사원은 잠금 자체가 성립하지 않는다
        assertThat(response.lockingFlag).isFalse()
        assertThat(response.appLoginActive).isTrue()
    }

    @Test
    @DisplayName("Trigger 부수 효과 - 퇴직 판촉직 여사원 -> 보호 미적용, 잠금 반영")
    fun update_retiredFieldStaff_lockingApplied() {
        val existing = Employee(id = 15L, employeeCode = "100600", name = "퇴직여사원")
            .apply {
                origin = EmployeeOrigin.MANUAL
                jobCode = "판촉직"
                status = "퇴직"
                role = AppAuthority.WOMAN
                appLoginActive = true
                lockingFlag = false
            }
        every { employeeRepository.findWithEmployeeInfoById(15L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val response = service.update(15L, AdminEmployeeUpdateRequest(lockingFlag = true))

        assertThat(response.lockingFlag).isTrue()
        assertThat(response.appLoginActive).isFalse()
    }

    @Test
    @DisplayName("costCenterCode 변경 시 매칭 User 의 derived 캐시도 동기화")
    fun update_syncsUserCostCenterCode() {
        val existing = Employee(id = 13L, employeeCode = "100400", name = "조직이동")
            .apply {
                origin = EmployeeOrigin.MANUAL
                costCenterCode = "1000"
            }
        val user = User(
            username = "u@otoki.local",
            employeeCode = "100400",
            password = "x",
            costCenterCode = "1000",
        )
        every { employeeRepository.findWithEmployeeInfoById(13L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }
        every { userRepository.findByEmployeeCode("100400") } returns user

        service.update(13L, AdminEmployeeUpdateRequest(costCenterCode = "2000"))

        assertThat(existing.costCenterCode).isEqualTo("2000")
        assertThat(user.costCenterCode).isEqualTo("2000")
    }

    @Test
    @DisplayName("전문행사조 - 정식 5개 조 표시명이면 해당 enum 으로 갱신")
    fun update_promotionTeam_assignsEnum() {
        val existing = promotionTeamEmployee(current = null)
        every { employeeRepository.findWithEmployeeInfoById(30L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        service.update(30L, AdminEmployeeUpdateRequest(professionalPromotionTeam = "라면세일조"))

        assertThat(existing.professionalPromotionTeam)
            .isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
    }

    @Test
    @DisplayName("전문행사조 - '일반'(미배정) 은 명시적 해제 신호라 null 로 저장")
    fun update_promotionTeam_generalClearsToNull() {
        val existing = promotionTeamEmployee(current = ProfessionalPromotionTeamType.RAMEN_SALE)
        every { employeeRepository.findWithEmployeeInfoById(30L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        service.update(
            30L,
            AdminEmployeeUpdateRequest(
                professionalPromotionTeam = ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME,
            ),
        )

        // 신규 시스템의 미배정은 null — 레거시 잔존 문자열 '일반' 을 새로 쓰지 않는다.
        assertThat(existing.professionalPromotionTeam).isNull()
    }

    @Test
    @DisplayName("전문행사조 - 미전송(null)/공백은 partial update 규칙대로 기존 값 유지")
    fun update_promotionTeam_absentKeepsCurrent() {
        val existing = promotionTeamEmployee(current = ProfessionalPromotionTeamType.CURRY_PROMOTION)
        every { employeeRepository.findWithEmployeeInfoById(30L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        service.update(30L, AdminEmployeeUpdateRequest(jikchak = "다른필드만"))
        assertThat(existing.professionalPromotionTeam)
            .isEqualTo(ProfessionalPromotionTeamType.CURRY_PROMOTION)

        service.update(30L, AdminEmployeeUpdateRequest(professionalPromotionTeam = "  "))
        assertThat(existing.professionalPromotionTeam)
            .isEqualTo(ProfessionalPromotionTeamType.CURRY_PROMOTION)
    }

    @Test
    @DisplayName("전문행사조 - 표시명이 바뀐 유형의 이전 문자열('카레행사조')도 동일 enum 으로 매핑")
    fun update_promotionTeam_legacyAlias() {
        val existing = promotionTeamEmployee(current = null)
        every { employeeRepository.findWithEmployeeInfoById(30L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        service.update(30L, AdminEmployeeUpdateRequest(professionalPromotionTeam = "카레행사조"))

        assertThat(existing.professionalPromotionTeam)
            .isEqualTo(ProfessionalPromotionTeamType.CURRY_PROMOTION)
    }

    @Test
    @DisplayName("전문행사조 - 허용값 밖 문자열은 예외 (Trigger 허용값 검증 동등)")
    fun update_promotionTeam_invalidRejected() {
        val existing = promotionTeamEmployee(current = null)
        every { employeeRepository.findWithEmployeeInfoById(30L) } returns existing

        assertThatThrownBy {
            service.update(30L, AdminEmployeeUpdateRequest(professionalPromotionTeam = "없는조"))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("없는조")
    }

    /** 전문행사조 갱신 검증용 MANUAL 사원 — [current] 가 수정 전 배정 상태. */
    private fun promotionTeamEmployee(current: ProfessionalPromotionTeamType?) =
        Employee(id = 30L, employeeCode = "100500", name = "행사조테스트").apply {
            origin = EmployeeOrigin.MANUAL
            professionalPromotionTeam = current
        }

    @Test
    @DisplayName("updateEmployeeRole - origin=SAP 사원도 role 변경 성공 (일반 수정과 달리 차단 안 함)")
    fun updateEmployeeRole_sapOrigin_allowed() {
        val sapEmployee = Employee(id = 20L, employeeCode = "200100", name = "SAP여사원")
            .apply {
                origin = EmployeeOrigin.SAP
                role = null // 미지정 상태
                jikchak = "기존직책"
            }
        every { employeeRepository.findWithEmployeeInfoById(20L) } returns sapEmployee
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val response = service.updateEmployeeRole(
            20L,
            AdminEmployeeRoleUpdateRequest(role = AppAuthority.ACCOUNT_VIEW_ALL),
        )

        assertThat(response.role).isEqualTo(AppAuthority.ACCOUNT_VIEW_ALL)
        // role 외 필드는 건드리지 않는다 (SAP SoT 보존)
        assertThat(sapEmployee.jikchak).isEqualTo("기존직책")
    }

    @Test
    @DisplayName("updateEmployeeRole - 존재하지 않는 사원 -> EmployeeNotFoundException")
    fun updateEmployeeRole_notFound() {
        every { employeeRepository.findWithEmployeeInfoById(999L) } returns null

        assertThatThrownBy {
            service.updateEmployeeRole(999L, AdminEmployeeRoleUpdateRequest(role = AppAuthority.WOMAN))
        }.isInstanceOf(EmployeeNotFoundException::class.java)
    }
}
