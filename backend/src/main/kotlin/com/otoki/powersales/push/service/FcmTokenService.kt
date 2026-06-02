package com.otoki.powersales.push.service

import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * FCM 디바이스 토큰 등록/해제 서비스.
 *
 * 토큰은 `employee_info.fcm_token` 에 저장되며, 발송 시 수신 대상 식별에 사용된다.
 * - 등록(register): 로그인/자동로그인/토큰 갱신 시 인증 사용자의 토큰을 최신값으로 갱신.
 * - 해제(unregister): 로그아웃 시 null 로 비워 이전 사용자에게 푸시가 가지 않도록 함
 *   (공용/교체 단말 대비 — 레거시의 emp_uuid 기반 단말 검증과 동일 취지).
 */
@Service
class FcmTokenService(
    private val employeeRepository: EmployeeRepository
) {

    /**
     * 인증 사용자의 FCM 토큰을 등록/갱신한다.
     */
    @Transactional
    fun register(employeeId: Long, token: String) {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException()
        // 영속 entity 변경 → @Transactional commit 시 dirty checking 으로 UPDATE
        employee.fcmToken = token
    }

    /**
     * 인증 사용자의 FCM 토큰을 해제(null)한다.
     */
    @Transactional
    fun unregister(employeeId: Long) {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException()
        employee.fcmToken = null
    }
}
