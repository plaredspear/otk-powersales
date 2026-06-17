package com.otoki.powersales.domain.activity.productexpiration.repository

import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ProductExpirationRepository : JpaRepository<ProductExpiration, Int>, ProductExpirationRepositoryCustom {

    fun countByEmployeeIdAndAlarmDate(employeeId: Long, alarmDate: LocalDate): Long

    /**
     * 알림 발송 대상 FCM 토큰(중복 제거) — 레거시 `fcmserverMapper.selectExpirationToken` 정합.
     *
     * `alarm_date` 가 해당일인 유통기한 레코드의 담당 여사원(employee) 디바이스 토큰
     * (`employee_info.fcm_token`) 을 조회한다. 토큰 미보유/공백은 제외 (레거시 `emp_token is not null
     * AND emp_token != ''` 정합). 한 여사원이 같은 날 여러 건 등록해도 DISTINCT 로 1회만 발송한다.
     */
    @Query(
        """
        SELECT DISTINCT ei.fcmToken
        FROM ProductExpiration pe
        JOIN pe.employee e
        JOIN e.employeeInfo ei
        WHERE pe.alarmDate = :alarmDate
          AND ei.fcmToken IS NOT NULL
          AND ei.fcmToken <> ''
        """
    )
    fun findDistinctFcmTokensByAlarmDate(@Param("alarmDate") alarmDate: LocalDate): List<String>

    fun findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeId: Long, fromDate: LocalDate, toDate: LocalDate
    ): List<ProductExpiration>

    fun findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeId: Long, accountCode: String, fromDate: LocalDate, toDate: LocalDate
    ): List<ProductExpiration>

    fun findByProductExpirationIdInAndEmployeeId(ids: List<Int>, employeeId: Long): List<ProductExpiration>
}
