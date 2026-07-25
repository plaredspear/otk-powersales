package com.otoki.powersales.platform.push.service

import com.otoki.powersales.domain.org.employee.repository.EmployeeInfoRepository
import com.otoki.powersales.platform.push.dto.PushTargetEmployee
import com.otoki.powersales.platform.push.sender.PushTarget
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 앱 아이콘 배지(미확인 푸시 건수) 관리 서비스.
 *
 * ## 왜 서버가 세는가
 * APNs 의 `aps.badge` 는 증분이 아니라 "표시할 절대값" 이고, 값을 싣지 않으면 배지는 변하지 않는다.
 * 앱은 백그라운드/종료 상태에서 코드를 실행할 수 없어 스스로 증가시킬 수 없으므로(기기에서 세려면
 * Notification Service Extension + App Group 이 필요), 서버가 사용자별 누적 값을 계산해 보낸다.
 * Android 도 notification 페이로드는 백그라운드에서 `onMessageReceived` 를 태우지 않아
 * 숫자를 앱이 계산할 수 없으므로 동일 값을 `notification_count` 로 함께 싣는다.
 *
 * ## 카운터 기준
 * 공지 읽음 기록 테이블이 없어 "미읽음" 을 파생할 수 없으므로 "미확인" 기준으로 센다:
 * 발송 시 +1, 사용자가 앱을 포그라운드로 열면([clear]) 0, 로그아웃 시에도 0.
 * 추후 읽음 상태가 생기면 [increaseAndGet] 을 파생 계산으로 교체하면 된다.
 */
@Service
class PushBadgeService(
    private val employeeInfoRepository: EmployeeInfoRepository,
) {

    /**
     * 발송 대상들의 배지를 1 증가시키고, 그 결과값을 실은 발송 대상 목록을 만든다.
     *
     * 배지 조회에 실패한 사원(EmployeeInfo 미보유 등)은 badge=null 로 두어 배지 payload 없이
     * 알림만 발송한다 — 배지 때문에 알림 자체를 누락시키지는 않는다.
     *
     * @param targets 발송 대상 (사원ID + 토큰)
     * @return 배지 절대값이 채워진 발송 대상 목록 (입력 순서 유지)
     */
    @Transactional
    fun increaseAndBuildTargets(targets: List<PushTargetEmployee>): List<PushTarget> {
        if (targets.isEmpty()) return emptyList()

        val employeeIds = targets.map { it.employeeId }.distinct()
        employeeInfoRepository.increasePushBadgeCount(employeeIds)
        val badges = employeeInfoRepository.findPushBadgeCounts(employeeIds)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toInt() }

        return targets.map { PushTarget(token = it.token, badge = badges[it.employeeId]) }
    }

    /** 사원의 배지 카운터를 0 으로 리셋한다 (앱 포그라운드 진입 / 로그아웃). */
    @Transactional
    fun clear(employeeId: Long) {
        employeeInfoRepository.clearPushBadgeCount(employeeId)
    }
}
