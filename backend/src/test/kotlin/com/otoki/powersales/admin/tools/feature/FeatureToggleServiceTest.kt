package com.otoki.powersales.admin.tools.feature

import com.otoki.powersales.admin.tools.feature.service.FeatureToggleService
import com.otoki.powersales.admin.tools.feature.service.FeatureToggleState
import com.otoki.powersales.admin.tools.feature.service.FeatureToggleStore
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.Optional

@DisplayName("FeatureToggleService 테스트")
class FeatureToggleServiceTest {

    private val store: FeatureToggleStore = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val service = FeatureToggleService(store, employeeRepository)

    @BeforeEach
    fun setUpDefaults() {
        // 대부분의 케이스는 예외 사번이 없는 상태를 전제한다.
        every { store.getExemptEmployeeCodes(any()) } returns emptyList()
    }

    private fun employee(id: Long, code: String?, name: String = "홍길동") =
        Employee(id = id, employeeCode = code, name = name)

    @Test
    @DisplayName("list - 전체 flag 상태를 label 과 함께 반환")
    fun list_returnsAllFlags() {
        every { store.getState(any()) } returns FeatureToggleState(enabled = true, reason = null)
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = "점검 중")

        val result = service.list()

        assertThat(result).hasSize(FeatureFlag.entries.size)
        assertThat(result.map { it.code })
            .containsExactly(
                "PRODUCT_CLAIM",
                "LOGISTICS_CLAIM",
                "ORDER_REQUEST",
                "ATTENDANCE_SCHEDULE_OWNER_DATE_CHECK",
            )
        val order = result.first { it.code == "ORDER_REQUEST" }
        assertThat(order.label).isEqualTo("주문 등록")
        assertThat(order.enabled).isFalse()
        assertThat(order.reason).isEqualTo("점검 중")
    }

    @Test
    @DisplayName("setEnabled - store 에 위임 후 최신 상태 반환")
    fun setEnabled_delegatesAndReturnsLatest() {
        every { store.setState(FeatureFlag.PRODUCT_CLAIM, false, "잠시 중지") } returns Unit
        every { store.getState(FeatureFlag.PRODUCT_CLAIM) } returns
            FeatureToggleState(enabled = false, reason = "잠시 중지")

        val result = service.setEnabled(FeatureFlag.PRODUCT_CLAIM, false, "잠시 중지")

        assertThat(result.enabled).isFalse()
        assertThat(result.reason).isEqualTo("잠시 중지")
        verify(exactly = 1) { store.setState(FeatureFlag.PRODUCT_CLAIM, false, "잠시 중지") }
    }

    @Test
    @DisplayName("ensureEnabled - 활성이면 사번 조회 없이 통과")
    fun ensureEnabled_passesWhenEnabled() {
        every { store.getState(FeatureFlag.PRODUCT_CLAIM) } returns
            FeatureToggleState(enabled = true, reason = null)

        assertThatCode { service.ensureEnabled(FeatureFlag.PRODUCT_CLAIM, 1L) }
            .doesNotThrowAnyException()

        verify(exactly = 0) { employeeRepository.findById(any()) }
    }

    @Test
    @DisplayName("ensureEnabled - 비활성 + 사유면 사유를 메시지로 409")
    fun ensureEnabled_throwsWithReason() {
        every { store.getState(FeatureFlag.LOGISTICS_CLAIM) } returns
            FeatureToggleState(enabled = false, reason = "물류 시스템 점검 중")
        every { employeeRepository.findById(1L) } returns
            Optional.of(employee(id = 1L, code = "12345678"))
        every { store.isExemptEmployee(FeatureFlag.LOGISTICS_CLAIM, "12345678") } returns false

        assertThatThrownBy { service.ensureEnabled(FeatureFlag.LOGISTICS_CLAIM, 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("물류 시스템 점검 중")
            .extracting { (it as BusinessException).httpStatus }
            .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @DisplayName("ensureEnabled - 비활성 + 사유 없으면 기본 문구로 409")
    fun ensureEnabled_throwsWithDefaultMessage() {
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = null)
        every { employeeRepository.findById(1L) } returns
            Optional.of(employee(id = 1L, code = "12345678"))
        every { store.isExemptEmployee(FeatureFlag.ORDER_REQUEST, "12345678") } returns false

        assertThatThrownBy { service.ensureEnabled(FeatureFlag.ORDER_REQUEST, 1L) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("주문 등록")
            .hasMessageContaining("중지")
    }

    @Test
    @DisplayName("ensureEnabled - 비활성이어도 예외 사번이면 통과")
    fun ensureEnabled_passesForExemptEmployee() {
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = "주문 중지")
        every { employeeRepository.findById(7L) } returns
            Optional.of(employee(id = 7L, code = "99999999"))
        every { store.isExemptEmployee(FeatureFlag.ORDER_REQUEST, "99999999") } returns true

        assertThatCode { service.ensureEnabled(FeatureFlag.ORDER_REQUEST, 7L) }
            .doesNotThrowAnyException()
    }

    @Test
    @DisplayName("isEnabled - 활성이면 사번 조회 없이 true (예외를 던지지 않는다)")
    fun isEnabled_trueWhenEnabled() {
        val flag = FeatureFlag.ATTENDANCE_SCHEDULE_OWNER_DATE_CHECK
        every { store.getState(flag) } returns FeatureToggleState(enabled = true, reason = null)

        assertThat(service.isEnabled(flag, 1L)).isTrue()

        verify(exactly = 0) { employeeRepository.findById(any()) }
    }

    @Test
    @DisplayName("isEnabled - 비활성이면 false (요청을 거부하지 않고 분기 판정만)")
    fun isEnabled_falseWhenDisabled() {
        val flag = FeatureFlag.ATTENDANCE_SCHEDULE_OWNER_DATE_CHECK
        every { store.getState(flag) } returns FeatureToggleState(enabled = false, reason = "롤백")
        every { employeeRepository.findById(1L) } returns
            Optional.of(employee(id = 1L, code = "12345678"))
        every { store.isExemptEmployee(flag, "12345678") } returns false

        assertThat(service.isEnabled(flag, 1L)).isFalse()
    }

    @Test
    @DisplayName("isEnabled - 비활성이어도 예외 사번이면 true (전사 롤백 중 특정 사원만 신규 동작 유지)")
    fun isEnabled_trueForExemptEmployee() {
        val flag = FeatureFlag.ATTENDANCE_SCHEDULE_OWNER_DATE_CHECK
        every { store.getState(flag) } returns FeatureToggleState(enabled = false, reason = "롤백")
        every { employeeRepository.findById(7L) } returns
            Optional.of(employee(id = 7L, code = "99999999"))
        every { store.isExemptEmployee(flag, "99999999") } returns true

        assertThat(service.isEnabled(flag, 7L)).isTrue()
    }

    @Test
    @DisplayName("ensureEnabled - 예외 사번은 flag 별로 독립 (주문 예외가 클레임을 열어주지 않음)")
    fun ensureEnabled_exemptionIsPerFlag() {
        every { store.getState(FeatureFlag.PRODUCT_CLAIM) } returns
            FeatureToggleState(enabled = false, reason = null)
        every { employeeRepository.findById(7L) } returns
            Optional.of(employee(id = 7L, code = "99999999"))
        every { store.isExemptEmployee(FeatureFlag.PRODUCT_CLAIM, "99999999") } returns false

        assertThatThrownBy { service.ensureEnabled(FeatureFlag.PRODUCT_CLAIM, 7L) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("ensureEnabled - 사번 없는 사원은 예외 대상이 아니라 차단")
    fun ensureEnabled_blocksEmployeeWithoutCode() {
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = null)
        every { employeeRepository.findById(9L) } returns
            Optional.of(employee(id = 9L, code = null))

        assertThatThrownBy { service.ensureEnabled(FeatureFlag.ORDER_REQUEST, 9L) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("list - 예외 사번을 사원명과 함께 반환")
    fun list_includesExemptEmployeesWithNames() {
        every { store.getState(any()) } returns FeatureToggleState(enabled = true, reason = null)
        every { store.getExemptEmployeeCodes(any()) } returns emptyList()
        every { store.getExemptEmployeeCodes(FeatureFlag.ORDER_REQUEST) } returns
            listOf("11111111", "22222222")
        every { employeeRepository.findByEmployeeCodeIn(listOf("11111111", "22222222")) } returns
            listOf(employee(id = 1L, code = "11111111", name = "김주문"))

        val order = service.list().first { it.code == "ORDER_REQUEST" }

        assertThat(order.exemptEmployees).hasSize(2)
        assertThat(order.exemptEmployees[0].employeeCode).isEqualTo("11111111")
        assertThat(order.exemptEmployees[0].name).isEqualTo("김주문")
        // 사원이 삭제되어 조회되지 않으면 사번만 남고 name 은 null.
        assertThat(order.exemptEmployees[1].name).isNull()
    }

    @Test
    @DisplayName("addExemptEmployee - 존재하는 사번이면 store 에 추가")
    fun addExemptEmployee_addsExistingCode() {
        every { employeeRepository.existsByEmployeeCode("12345678") } returns true
        every { store.addExemptEmployee(FeatureFlag.ORDER_REQUEST, "12345678") } returns Unit
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = null)
        every { store.getExemptEmployeeCodes(FeatureFlag.ORDER_REQUEST) } returns listOf("12345678")
        every { employeeRepository.findByEmployeeCodeIn(listOf("12345678")) } returns
            listOf(employee(id = 1L, code = "12345678", name = "김주문"))

        // 공백이 섞여 입력돼도 trim 후 저장한다.
        val result = service.addExemptEmployee(FeatureFlag.ORDER_REQUEST, "  12345678 ")

        assertThat(result.exemptEmployees.map { it.employeeCode }).containsExactly("12345678")
        verify(exactly = 1) { store.addExemptEmployee(FeatureFlag.ORDER_REQUEST, "12345678") }
    }

    @Test
    @DisplayName("addExemptEmployee - 없는 사번이면 400")
    fun addExemptEmployee_rejectsUnknownCode() {
        every { employeeRepository.existsByEmployeeCode("00000000") } returns false

        assertThatThrownBy { service.addExemptEmployee(FeatureFlag.ORDER_REQUEST, "00000000") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).httpStatus }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        verify(exactly = 0) { store.addExemptEmployee(any(), any()) }
    }

    @Test
    @DisplayName("removeExemptEmployee - store 에서 제거 후 최신 상태 반환")
    fun removeExemptEmployee_removesCode() {
        every { store.removeExemptEmployee(FeatureFlag.ORDER_REQUEST, "12345678") } returns Unit
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = null)
        every { store.getExemptEmployeeCodes(FeatureFlag.ORDER_REQUEST) } returns emptyList()

        val result = service.removeExemptEmployee(FeatureFlag.ORDER_REQUEST, "12345678")

        assertThat(result.exemptEmployees).isEmpty()
        verify(exactly = 1) { store.removeExemptEmployee(FeatureFlag.ORDER_REQUEST, "12345678") }
    }
}
