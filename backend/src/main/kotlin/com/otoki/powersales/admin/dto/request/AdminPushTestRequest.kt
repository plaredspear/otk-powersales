package com.otoki.powersales.admin.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * FCM push 발송 테스트 요청 (개발자 도구 > 외부 API 테스트).
 *
 * 사번(employeeCode)에 등록된 FCM 토큰으로 임의 제목/본문의 테스트 알림을 1건 발송한다.
 * 실제 발송은 `app.push.fcm.enabled=true` + credential 주입 + 운영(`!local`) 프로필에서만 이루어지며,
 * local 프로필은 StubFcmSender 라 로그만 남고 발송되지 않는다.
 */
data class AdminPushTestRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Size(min = 1, max = 20, message = "사번은 1~20자 이내여야 합니다")
    val employeeCode: String,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(min = 1, max = 100, message = "제목은 1~100자 이내여야 합니다")
    val title: String,

    @field:NotBlank(message = "본문은 필수입니다")
    @field:Size(min = 1, max = 200, message = "본문은 1~200자 이내여야 합니다")
    val body: String,
)
