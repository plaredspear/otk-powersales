package com.otoki.powersales.platform.push.dto

/**
 * 푸시 발송 대상 사원 (사원ID + 디바이스 토큰) 조회 projection.
 *
 * 배지 값은 사원별로 달라 발송 직전에 사원 단위로 계산해야 하므로, 대상 조회는 토큰만이 아니라
 * 사원ID 를 함께 돌려준다 ([com.otoki.powersales.platform.push.service.PushBadgeService] 입력).
 *
 * @property employeeId 대상 사원 ID
 * @property token 대상 디바이스 FCM 토큰
 */
data class PushTargetEmployee(
    val employeeId: Long,
    val token: String,
)
