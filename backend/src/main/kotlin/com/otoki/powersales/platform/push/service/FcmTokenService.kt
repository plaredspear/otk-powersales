package com.otoki.powersales.platform.push.service

import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.repository.EmployeeInfoRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * FCM 디바이스 토큰 등록/해제 서비스.
 *
 * 토큰은 `employee_info.fcm_token` 에 저장되며, 발송 시 수신 대상 식별에 사용된다.
 * - 등록(register): 로그인/자동로그인/토큰 갱신 시 인증 사용자의 토큰을 최신값으로 갱신.
 *   같은 토큰을 보유한 다른 사원이 있으면 그쪽을 먼저 해제해 소유권을 이전한다.
 * - 해제(unregister): 로그아웃 시 null 로 비워 이전 사용자에게 푸시가 가지 않도록 함
 *   (공용/교체 단말 대비 — 레거시의 emp_uuid 기반 단말 검증과 동일 취지).
 *
 * 불변식: **사원당 토큰 1개(한 계정 한 단말) + 토큰당 사원 1명(전역 유일)**.
 * 전자는 단일 컬럼 구조가, 후자는 [register] 의 사전 해제가 보장한다.
 */
@Service
class FcmTokenService(
    private val employeeRepository: EmployeeRepository,
    private val employeeInfoRepository: EmployeeInfoRepository,
    private val pushBadgeService: PushBadgeService
) {

    /**
     * 인증 사용자의 FCM 토큰을 등록/갱신한다.
     *
     * 토큰은 사원당 1개(= 한 계정 한 단말)이며 동시에 전역 유일해야 한다. 따라서 등록 전에
     * 같은 토큰을 보유한 다른 사원의 값을 먼저 해제해 소유권을 이전한다 — 로그아웃 없이
     * 계정을 바꿔 로그인한 단말에서 이전 사원 앞으로도 푸시가 가는 것을 막는다.
     *
     * 순서 주의: 타 사원 해제는 벌크 UPDATE(clearAutomatically)라 영속성 컨텍스트를 비우므로,
     * 반드시 본인 entity 를 로딩하기 **전에** 실행한다. 순서를 뒤집으면 detach 된 본인 entity
     * 변경이 dirty checking 대상에서 빠져 토큰이 저장되지 않는다.
     */
    @Transactional
    fun register(employeeId: Long, token: String) {
        employeeInfoRepository.releaseFcmTokenFromOtherEmployees(token, employeeId)

        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException()
        // 영속 entity 변경 → @Transactional commit 시 dirty checking 으로 UPDATE
        employee.fcmToken = token
    }

    /**
     * 인증 사용자의 FCM 토큰을 해제(null)한다.
     *
     * 배지 카운터도 함께 0 으로 리셋한다 — 로그아웃 후 다음 사용자가 같은 단말에 로그인했을 때
     * 이전 사용자의 미확인 건수가 배지에 이어지지 않게 한다.
     */
    @Transactional
    fun unregister(employeeId: Long) {
        val employee = employeeRepository.findWithEmployeeInfoById(employeeId)
            ?: throw EmployeeNotFoundException()
        employee.fcmToken = null
        pushBadgeService.clear(employeeId)
    }
}
