package com.otoki.powersales.domain.activity.productexpiration.service

import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import com.otoki.powersales.platform.push.sender.FcmSender
import com.otoki.powersales.platform.push.service.PushBadgeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 유통기한 만료 알림 발송 service.
 *
 * ## 레거시 매핑 (`OttogiSalesSchedule.send("alarm")`)
 * 매일 `alarm_date` 가 당일인 유통기한 레코드의 담당 여사원 FCM 토큰을 모아 동일 notification 푸시를 발송한다.
 * - 대상 조회: 레거시 `fcmserverMapper.selectExpirationToken` 정합 ([ProductExpirationRepository.findDistinctFcmTokensByAlarmDate]).
 * - 발송: Firebase Admin SDK (HTTP v1) — 레거시는 종료된 FCM Legacy HTTP key API 였으나 신규는 v1 로 대체.
 * - 알림 이력 전용 테이블 없음 (레거시 정합). 발송 집계는 배치 `scheduled_job_run` metadata 로 기록.
 */
@Service
@Transactional(readOnly = true)
class ProductExpirationAlertService(
    private val productExpirationRepository: ProductExpirationRepository,
    private val fcmSender: FcmSender,
    private val pushBadgeService: PushBadgeService,
) {

    // 배지 카운터를 증가시키므로 클래스 기본값(readOnly)이 아닌 쓰기 트랜잭션이 필요하다.
    @Transactional
    fun sendDailyAlerts(today: LocalDate): ProductExpirationAlertResult {
        val targets = productExpirationRepository.findDistinctPushTargetsByAlarmDate(today)
        if (targets.isEmpty()) return ProductExpirationAlertResult(0, 0, 0)

        // 공지 push 와 동일하게 대상별 배지(미확인 푸시 건수)를 +1 한 절대값을 함께 싣는다.
        val result = fcmSender.sendNotification(
            targets = pushBadgeService.increaseAndBuildTargets(targets),
            title = ALERT_TITLE,
            body = ALERT_BODY,
        )
        return ProductExpirationAlertResult(
            targetTokens = targets.size,
            successCount = result.successCount,
            failureCount = result.failureCount,
        )
    }

    companion object {
        /** 레거시 notification 제목 — "유통기한 " 뒤 공백 2칸은 레거시 원문 그대로. */
        const val ALERT_TITLE = "유통기한  임박"
        const val ALERT_BODY = "오늘 유통기한 임박제품이 있습니다."
    }
}

/**
 * 발송 결과 집계.
 *
 * @property targetTokens 발송 대상 토큰 수
 * @property successCount 발송 성공 수
 * @property failureCount 발송 실패 수
 */
data class ProductExpirationAlertResult(
    val targetTokens: Int,
    val successCount: Int,
    val failureCount: Int,
)
