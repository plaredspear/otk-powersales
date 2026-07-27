package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.entity.EmployeeInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * employee_info 직접 접근 repository.
 *
 * Employee 를 거치지 않고 employee_info 만 갱신해야 하는 경우(앱 배지 카운터)에 사용한다.
 * 배지 갱신은 발송 대상이 수백~수천 건일 수 있어, entity 로딩 + dirty checking(N UPDATE) 대신
 * 단일 벌크 UPDATE 로 처리한다.
 */
interface EmployeeInfoRepository : JpaRepository<EmployeeInfo, Long> {

    /**
     * 대상 사원들의 미확인 푸시 건수를 1 증가시킨다 (푸시 발송 시점).
     *
     * 벌크 UPDATE 는 영속성 컨텍스트를 우회하므로, 이후 조회가 옛 값을 보지 않도록
     * flush/clear 를 동반한다 (호출 트랜잭션의 다른 entity 는 detach 됨에 유의).
     *
     * @return 갱신된 행 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE EmployeeInfo ei
           SET ei.pushBadgeCount = ei.pushBadgeCount + 1
         WHERE ei.employeeId IN :employeeIds
        """
    )
    fun increasePushBadgeCount(@Param("employeeIds") employeeIds: Collection<Long>): Int

    /**
     * 대상 사원들의 현재 미확인 푸시 건수를 조회한다 (발송 payload 에 실을 배지 절대값).
     *
     * @return [사원ID, 배지값] 2열 배열 목록
     */
    @Query(
        """
        SELECT ei.employeeId, ei.pushBadgeCount
          FROM EmployeeInfo ei
         WHERE ei.employeeId IN :employeeIds
        """
    )
    fun findPushBadgeCounts(@Param("employeeIds") employeeIds: Collection<Long>): List<Array<Any>>

    /**
     * 사원의 미확인 푸시 건수를 0 으로 리셋한다 (앱 포그라운드 진입 / 로그아웃).
     *
     * @return 갱신된 행 수 (EmployeeInfo 미보유 사원이면 0)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE EmployeeInfo ei
           SET ei.pushBadgeCount = 0
         WHERE ei.employeeId = :employeeId
           AND ei.pushBadgeCount <> 0
        """
    )
    fun clearPushBadgeCount(@Param("employeeId") employeeId: Long): Int

    /**
     * 지정 토큰을 보유한 **다른** 사원들의 fcmToken 을 해제(null)한다 (토큰 등록 시점).
     *
     * FCM 토큰은 단말 1대에 1개이고 본 서비스는 "한 계정 = 한 단말" 기준이므로, 하나의 토큰이
     * 두 사원에 동시에 매달려서는 안 된다. 그러나 로그아웃 없이 계정을 바꿔 로그인하면
     * (앱 삭제 후 재설치 / 세션 만료 / 강제 로그아웃) 이전 사원의 행에 같은 토큰이 그대로 남아,
     * 같은 단말로 두 사원분의 푸시가 발송될 수 있다. 새 사원이 토큰을 등록하는 시점에
     * 이전 소유자의 토큰을 비워 소유권을 이전한다.
     *
     * @return 해제된 행 수 (정상 흐름에서는 0, 로그아웃 없이 계정 전환 시 1)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE EmployeeInfo ei
           SET ei.fcmToken = NULL
         WHERE ei.fcmToken = :token
           AND ei.employeeId <> :employeeId
        """
    )
    fun releaseFcmTokenFromOtherEmployees(
        @Param("token") token: String,
        @Param("employeeId") employeeId: Long
    ): Int
}
