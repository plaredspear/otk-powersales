package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminPushTestRequest
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.push.sender.FcmSender
import com.otoki.powersales.platform.push.sender.FcmSendResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AdminPushTestService 테스트")
class AdminPushTestServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val fcmSender: FcmSender = mockk()
    private val service = AdminPushTestService(employeeRepository, fcmSender)

    private fun request(
        employeeCode: String = "00012345",
        title: String = "테스트 알림",
        body: String = "본문",
    ) = AdminPushTestRequest(employeeCode = employeeCode, title = title, body = body)

    private fun employeeWith(name: String, token: String?): Employee {
        val employee: Employee = mockk(relaxed = true)
        every { employee.name } returns name
        every { employee.fcmToken } returns token
        return employee
    }

    @Nested
    @DisplayName("test - 사번 대상 push 발송")
    inner class TestPush {

        @Test
        @DisplayName("사원 미존재 - 발송 없이 tokenRegistered=false + 안내 메시지")
        fun test_employeeNotFound() {
            every { employeeRepository.findWithEmployeeInfoByEmployeeCode("99999999") } returns null

            val result = service.test(request(employeeCode = "99999999"))

            assertThat(result.tokenRegistered).isFalse()
            assertThat(result.employeeName).isNull()
            assertThat(result.successCount).isZero()
            assertThat(result.message).contains("사원을 찾을 수 없습니다")
            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("FCM 토큰 미등록 - 발송 없이 tokenRegistered=false + 안내 메시지")
        fun test_tokenNotRegistered() {
            every { employeeRepository.findWithEmployeeInfoByEmployeeCode("00012345") } returns
                employeeWith(name = "홍길동", token = null)

            val result = service.test(request())

            assertThat(result.tokenRegistered).isFalse()
            assertThat(result.employeeName).isEqualTo("홍길동")
            assertThat(result.maskedToken).isNull()
            assertThat(result.message).contains("FCM 토큰이 없습니다")
            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("FCM 토큰 blank - 미등록으로 처리")
        fun test_tokenBlank() {
            every { employeeRepository.findWithEmployeeInfoByEmployeeCode("00012345") } returns
                employeeWith(name = "홍길동", token = "   ")

            val result = service.test(request())

            assertThat(result.tokenRegistered).isFalse()
            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("발송 성공 - 토큰 1건으로 FcmSender 호출 + success 집계/마스킹 토큰 반환")
        fun test_sendSuccess() {
            val token = "abcdefgh_1234567890_token_value"
            every { employeeRepository.findWithEmployeeInfoByEmployeeCode("00012345") } returns
                employeeWith(name = "홍길동", token = token)
            val tokensSlot = slot<List<String>>()
            val dataSlot = slot<Map<String, String>>()
            every {
                fcmSender.sendNotificationToTokens(capture(tokensSlot), "테스트 알림", "본문", capture(dataSlot))
            } returns FcmSendResult(successCount = 1, failureCount = 0)

            val result = service.test(request())

            assertThat(result.tokenRegistered).isTrue()
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isZero()
            assertThat(result.maskedToken).startsWith("abcdefgh").contains("${token.length}자")
            assertThat(result.message).contains("발송 성공")
            assertThat(tokensSlot.captured).containsExactly(token)
            assertThat(dataSlot.captured).containsEntry("type", "push-test")
        }

        @Test
        @DisplayName("발송 실패 집계 - failure>0/success=0 -> 실패 메시지")
        fun test_sendFailure() {
            every { employeeRepository.findWithEmployeeInfoByEmployeeCode("00012345") } returns
                employeeWith(name = "홍길동", token = "token-value-abcdef")
            every {
                fcmSender.sendNotificationToTokens(any(), any(), any(), any())
            } returns FcmSendResult(successCount = 0, failureCount = 1)

            val result = service.test(request())

            assertThat(result.tokenRegistered).isTrue()
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.message).contains("발송 실패")
        }

        @Test
        @DisplayName("미발송(EMPTY) - FCM 비활성/Stub 상황 -> 미수행 메시지")
        fun test_notSent() {
            every { employeeRepository.findWithEmployeeInfoByEmployeeCode("00012345") } returns
                employeeWith(name = "홍길동", token = "token-value-abcdef")
            every {
                fcmSender.sendNotificationToTokens(any(), any(), any(), any())
            } returns FcmSendResult.EMPTY

            val result = service.test(request())

            assertThat(result.tokenRegistered).isTrue()
            assertThat(result.successCount).isZero()
            assertThat(result.failureCount).isZero()
            assertThat(result.message).contains("발송이 수행되지 않았습니다")
        }
    }
}
