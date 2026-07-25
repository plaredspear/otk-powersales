package com.otoki.powersales.domain.activity.productexpiration.repository

import com.otoki.powersales.domain.activity.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import com.otoki.powersales.platform.push.dto.PushTargetEmployee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface ProductExpirationRepositoryCustom {

    fun findForAdmin(
        fromDate: LocalDate?,
        toDate: LocalDate?,
        employeeKeyword: String?,
        accountKeyword: String?,
        status: String?,
        today: LocalDate,
        pageable: Pageable,
        employeeIds: List<Long>? = null
    ): Page<ProductExpiration>

    fun getSummary(today: LocalDate, employeeIds: List<Long>? = null): AdminProductExpirationSummaryResponse

    /**
     * 알림 발송 대상 사원(사원ID + FCM 토큰, 중복 제거) — 레거시 `fcmserverMapper.selectExpirationToken` 정합.
     *
     * `alarm_date` 가 해당일인 유통기한 레코드의 담당 여사원(employee) 디바이스 토큰
     * (`employee_info.fcm_token`) 을 조회한다. 토큰 미보유/공백은 제외 (레거시 `emp_token is not null
     * AND emp_token != ''` 정합). 한 여사원이 같은 날 여러 건 등록해도 DISTINCT 로 1회만 발송한다.
     *
     * 배지(미확인 푸시 건수) 계산에 사원 단위 식별이 필요해 토큰과 함께 사원ID 를 반환한다.
     */
    fun findDistinctPushTargetsByAlarmDate(alarmDate: LocalDate): List<PushTargetEmployee>
}
