package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminPushTestRequest
import com.otoki.powersales.admin.dto.response.AdminPushTestResponse
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.push.sender.FcmSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * FCM push 발송 테스트 서비스 (개발자 도구 > 외부 API 테스트).
 *
 * 사번으로 사원을 조회해 등록된 FCM 토큰으로 임의 제목/본문의 테스트 알림을 1건 발송한다.
 * data payload 는 공지 push 와 동일한 `type` 키 규약을 따르되 `push-test` 타입으로 구분한다.
 *
 * 실제 발송 여부는 [FcmSender] 구현체(운영=RealFcmSender / local=StubFcmSender)와
 * `app.push.fcm.enabled` + credential 주입에 좌우된다. 토큰 미등록 사번은 발송 없이 결과만 반환한다.
 */
@Service
class AdminPushTestService(
    private val employeeRepository: EmployeeRepository,
    private val fcmSender: FcmSender,
) {

    companion object {
        private const val PUSH_TYPE_TEST = "push-test"
        private const val TOKEN_MASK_PREFIX_LENGTH = 8
    }

    @Transactional(readOnly = true)
    fun test(request: AdminPushTestRequest): AdminPushTestResponse {
        val employee = employeeRepository.findWithEmployeeInfoByEmployeeCode(request.employeeCode)
            ?: return AdminPushTestResponse(
                employeeCode = request.employeeCode,
                employeeName = null,
                tokenRegistered = false,
                maskedToken = null,
                successCount = 0,
                failureCount = 0,
                message = "해당 사번의 사원을 찾을 수 없습니다.",
            )

        val token = employee.fcmToken?.takeIf { it.isNotBlank() }
        if (token == null) {
            return AdminPushTestResponse(
                employeeCode = request.employeeCode,
                employeeName = employee.name,
                tokenRegistered = false,
                maskedToken = null,
                successCount = 0,
                failureCount = 0,
                message = "해당 사원에 등록된 FCM 토큰이 없습니다. 모바일 앱 로그인 후 다시 시도하세요.",
            )
        }

        val result = fcmSender.sendNotificationToTokens(
            tokens = listOf(token),
            title = request.title,
            body = request.body,
            data = mapOf("type" to PUSH_TYPE_TEST),
        )

        return AdminPushTestResponse(
            employeeCode = request.employeeCode,
            employeeName = employee.name,
            tokenRegistered = true,
            maskedToken = maskToken(token),
            successCount = result.successCount,
            failureCount = result.failureCount,
            message = buildMessage(result.successCount, result.failureCount),
        )
    }

    private fun maskToken(token: String): String {
        val prefix = token.take(TOKEN_MASK_PREFIX_LENGTH)
        return "$prefix…(${token.length}자)"
    }

    private fun buildMessage(successCount: Int, failureCount: Int): String =
        when {
            successCount > 0 -> "발송 성공 (success=$successCount, failure=$failureCount)"
            failureCount > 0 -> "발송 실패 — 토큰이 무효하거나 만료되었을 수 있습니다 (failure=$failureCount)"
            else -> "발송이 수행되지 않았습니다. FCM 비활성(app.push.fcm) 이거나 local(Stub) 프로필일 수 있습니다."
        }
}
