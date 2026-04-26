package com.otoki.powersales.productexpiration.service

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * ProductExpiration Entity가 V1 스키마로 리매핑되어 user.id, productName, alertSent 등
 * V2 전용 필드가 제거됨. 기존 스케줄 로직이 해당 필드를 참조하므로 주석 처리.

import com.otoki.powersales.productexpiration.repository.ProductExpirationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class ProductExpirationAlertService(
    private val productExpirationRepository: ProductExpirationRepository
) {

    private val log = LoggerFactory.getLogger(ProductExpirationAlertService::class.java)

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    fun sendExpiryAlerts() {
        val today = LocalDate.now()
        val alertTargets = productExpirationRepository.findByAlertDateAndAlertSentFalse(today)

        if (alertTargets.isEmpty()) {
            log.debug("오늘({}) 발송할 유통기한 알림이 없습니다", today)
            return
        }

        log.info("유통기한 알림 발송 시작: {}건", alertTargets.size)

        var successCount = 0
        var failCount = 0

        for (productExpiration in alertTargets) {
            try {
                val daysRemaining = ChronoUnit.DAYS.between(today, productExpiration.expiryDate)
                val title = "유통기한 알림"
                val body = "${productExpiration.productName}의 유통기한이 ${daysRemaining}일 남았습니다"

                log.info(
                    "알림 발송 - userId: {}, product: {}, daysRemaining: {}",
                    productExpiration.user.id, productExpiration.productName, daysRemaining
                )

                productExpiration.alertSent = true
                productExpirationRepository.save(productExpiration)
                successCount++
            } catch (e: Exception) {
                log.error(
                    "알림 발송 실패 - productExpirationId: {}, error: {}",
                    productExpiration.id, e.message
                )
                failCount++
            }
        }

        log.info("유통기한 알림 발송 완료: 성공 {}건, 실패 {}건", successCount, failCount)
    }
}

--- */
