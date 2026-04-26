package com.otoki.powersales.promotion.batch

import com.otoki.powersales.admin.service.AdminPPTMasterService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@EnableScheduling
@Profile("!local")
class PPTMasterBatchConfig(
    private val adminPPTMasterService: AdminPPTMasterService
) {

    private val log = LoggerFactory.getLogger(PPTMasterBatchConfig::class.java)

    /**
     * 새벽 배치 (PPTMasterSyncBatch)
     * 매일 05:00에 유효 마스터의 전문행사조를 사원에 동기화
     */
    @Scheduled(cron = "0 0 5 * * *")
    fun syncValidMasters() {
        log.info("PPTMasterSyncBatch 시작")
        try {
            adminPPTMasterService.syncValidMasters()
            log.info("PPTMasterSyncBatch 완료")
        } catch (e: Exception) {
            log.error("PPTMasterSyncBatch 실패", e)
        }
    }

    /**
     * 심야 배치 (PPTMasterExpireBatch)
     * 매일 23:00에 종료일 도래 마스터의 사원을 '일반'으로 복귀
     */
    @Scheduled(cron = "0 0 23 * * *")
    fun expireMasters() {
        log.info("PPTMasterExpireBatch 시작")
        try {
            adminPPTMasterService.expireMasters()
            log.info("PPTMasterExpireBatch 완료")
        } catch (e: Exception) {
            log.error("PPTMasterExpireBatch 실패", e)
        }
    }
}
