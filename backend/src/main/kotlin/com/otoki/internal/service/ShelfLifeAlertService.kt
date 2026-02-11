package com.otoki.internal.service

import com.otoki.internal.repository.ShelfLifeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 유통기한 알림 발송 스케줄 Service
 * 매일 오전 9시에 alertDate가 오늘이고 alertSent가 false인 항목에 대해
 * 푸시 알림을 발송하고 alertSent를 true로 업데이트한다.
 */
@Service
class ShelfLifeAlertService(
    private val shelfLifeRepository: ShelfLifeRepository
) {

    private val log = LoggerFactory.getLogger(ShelfLifeAlertService::class.java)

    /**
     * 유통기한 마감 전 알림 발송
     * 매일 오전 9시에 실행
     *
     * 1. alertDate가 오늘이고 alertSent가 false인 ShelfLife 목록 조회
     * 2. 각 항목에 대해 해당 사용자에게 푸시 알림 발송
     * 3. 발송 성공 시 alertSent를 true로 업데이트
     * 4. 발송 실패 시 alertSent를 false로 유지 (다음 실행 시 재시도)
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    fun sendExpiryAlerts() {
        val today = LocalDate.now()
        val alertTargets = shelfLifeRepository.findByAlertDateAndAlertSentFalse(today)

        if (alertTargets.isEmpty()) {
            log.debug("오늘({}) 발송할 유통기한 알림이 없습니다", today)
            return
        }

        log.info("유통기한 알림 발송 시작: {}건", alertTargets.size)

        var successCount = 0
        var failCount = 0

        for (shelfLife in alertTargets) {
            try {
                val daysRemaining = ChronoUnit.DAYS.between(today, shelfLife.expiryDate)
                val title = "유통기한 알림"
                val body = "${shelfLife.productName}의 유통기한이 ${daysRemaining}일 남았습니다"

                // TODO: Firebase Cloud Messaging (FCM) 연동
                // pushNotificationService.send(shelfLife.user.id, title, body)
                log.info(
                    "알림 발송 - userId: {}, product: {}, daysRemaining: {}",
                    shelfLife.user.id, shelfLife.productName, daysRemaining
                )

                shelfLife.alertSent = true
                shelfLifeRepository.save(shelfLife)
                successCount++
            } catch (e: Exception) {
                log.error(
                    "알림 발송 실패 - shelfLifeId: {}, error: {}",
                    shelfLife.id, e.message
                )
                failCount++
                // alertSent를 false로 유지하여 다음 실행 시 재시도
            }
        }

        log.info("유통기한 알림 발송 완료: 성공 {}건, 실패 {}건", successCount, failCount)
    }
}
