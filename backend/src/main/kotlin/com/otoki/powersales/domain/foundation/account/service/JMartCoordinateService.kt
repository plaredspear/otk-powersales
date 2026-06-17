package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.batch.config.JMartCoordinateProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * J마트(이동매장) 요일별 좌표 보정 서비스.
 *
 * 레거시 `Batch_JMartLatLong` 동등 — 대상 거래처(`externalKey`)의 좌표를 오늘 요일에 매칭되는 값으로 덮어쓴다.
 * 매칭 요일이 없으면 아무 것도 하지 않는다(레거시의 빈 else 분기 동등). 좌표는 dirty checking 으로 커밋 시 UPDATE.
 */
@Service
class JMartCoordinateService(
    private val accountRepository: AccountRepository,
    private val properties: JMartCoordinateProperties,
) {

    private val log = LoggerFactory.getLogger(JMartCoordinateService::class.java)

    /**
     * 오늘 요일에 매칭되는 좌표를 대상 거래처에 적용한다.
     *
     * @param today 적용 기준 요일 (기본 오늘)
     * @return 적용 결과
     */
    @Transactional
    fun applyTodayCoordinate(today: DayOfWeek = LocalDate.now().dayOfWeek): JMartCoordinateResult {
        val externalKey = properties.externalKey.takeIf { it.isNotBlank() }
            ?: return JMartCoordinateResult(applied = false, externalKey = properties.externalKey, label = null)

        val schedule = properties.scheduleFor(today)
            ?: return JMartCoordinateResult(applied = false, externalKey = externalKey, label = null)

        val account = accountRepository.findByExternalKey(externalKey)
        if (account == null) {
            log.warn("JMART_COORDINATE 대상 거래처 미존재 externalKey={}", externalKey)
            return JMartCoordinateResult(applied = false, externalKey = externalKey, label = schedule.label)
        }

        account.latitude = schedule.latitude
        account.longitude = schedule.longitude
        log.info(
            "JMART_COORDINATE_APPLIED externalKey={} day={} label={} lat={} lng={}",
            externalKey, today, schedule.label, schedule.latitude, schedule.longitude,
        )
        return JMartCoordinateResult(applied = true, externalKey = externalKey, label = schedule.label)
    }
}

data class JMartCoordinateResult(
    val applied: Boolean,
    val externalKey: String,
    val label: String?,
)
