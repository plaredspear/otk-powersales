package com.otoki.powersales.domain.activity.schedule.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.DayOfWeek

/**
 * 이동매장 요일별 좌표 예외의 Redis 저장/조회 —
 * `BranchScopeModeStore` / `FeatureToggleStore` 와 동일한 "기본값은 키 부재" 규약.
 *
 * - key `account_day_coordinate:1015773` → `"<DayOfWeek>|<lat>|<lng>|<label>"` 단일 문자열.
 *   키가 없으면 [AccountDayCoordinateOverride.DEFAULT_COORDINATE] (코드 상수) 를 쓴다.
 * - TTL 없이 영구 저장 → 앱 재시작 후에도 유지된다.
 * - Redis 미가동/장애/파싱 실패 시 조회는 **코드 상수 폴백** — 개발자 도구 값을 못 읽었다고 해서
 *   요일 예외가 통째로 사라지면 해당 요일 출근등록이 거리 초과로 전면 실패하기 때문이다.
 *
 * 값이 `|` 구분 단일 문자열인 이유: 대상이 거래처 1건 × 요일 1건뿐이라 JSON 직렬화 의존을 더할
 * 이유가 없다. 대상이 다건으로 늘어나면 그때 구조를 바꾼다 ([AccountDayCoordinateOverride] 확장 기준).
 */
@Component
class AccountDayCoordinateOverrideStore(
    /** Redis 미사용 환경 (test profile 등) 에서는 빈 미등록 — null 허용. */
    private val redisTemplate: RedisTemplate<String, String>?,
) {
    private val log = LoggerFactory.getLogger(AccountDayCoordinateOverrideStore::class.java)

    /**
     * 해당 거래처 / 요일에 적용할 좌표 예외를 조회한다.
     *
     * @return 매칭된 예외 좌표. 대상 거래처가 아니거나 설정된 요일과 다르면 null
     *         (호출자는 거래처 원본 좌표 경로를 그대로 탄다).
     */
    fun resolve(account: Account?, dayOfWeek: DayOfWeek): AccountDayCoordinateOverride.DayCoordinate? {
        if (!AccountDayCoordinateOverride.isTarget(account?.externalKey)) return null
        val current = getCoordinate()
        return current.takeIf { it.dayOfWeek == dayOfWeek }
    }

    /** 현재 적용 중인 예외 설정 (Redis 값 우선, 부재/실패 시 코드 상수). */
    fun getCoordinate(): AccountDayCoordinateOverride.DayCoordinate {
        val template = redisTemplate ?: return AccountDayCoordinateOverride.DEFAULT_COORDINATE
        return try {
            template.opsForValue().get(COORDINATE_KEY)?.let(::deserialize)
                ?: AccountDayCoordinateOverride.DEFAULT_COORDINATE
        } catch (e: Exception) {
            log.warn("이동매장 좌표 예외 조회 실패 → 코드 기본값 폴백", e)
            AccountDayCoordinateOverride.DEFAULT_COORDINATE
        }
    }

    /** 예외 설정을 저장한다. 코드 기본값과 같으면 키를 지운다. Redis 미가동이면 예외. */
    fun setCoordinate(coordinate: AccountDayCoordinateOverride.DayCoordinate) {
        val template = redisTemplate
            ?: throw IllegalStateException("Redis 미사용 환경에서는 이동매장 좌표 예외를 변경할 수 없습니다")
        if (coordinate == AccountDayCoordinateOverride.DEFAULT_COORDINATE) {
            template.delete(COORDINATE_KEY)
            return
        }
        template.opsForValue().set(COORDINATE_KEY, serialize(coordinate))
    }

    /** 저장된 값을 지워 코드 기본값으로 되돌린다. */
    fun reset() {
        val template = redisTemplate
            ?: throw IllegalStateException("Redis 미사용 환경에서는 이동매장 좌표 예외를 변경할 수 없습니다")
        template.delete(COORDINATE_KEY)
    }

    /** Redis 에 저장된 값이 있는지 (= 코드 기본값에서 벗어났는지). */
    fun isCustomized(): Boolean {
        val template = redisTemplate ?: return false
        return try {
            template.hasKey(COORDINATE_KEY)
        } catch (e: Exception) {
            log.warn("이동매장 좌표 예외 키 존재 확인 실패 → false 폴백", e)
            false
        }
    }

    private fun serialize(c: AccountDayCoordinateOverride.DayCoordinate): String =
        listOf(c.dayOfWeek.name, c.latitude.toString(), c.longitude.toString(), c.label).joinToString(DELIMITER)

    /**
     * 저장 문자열 → 좌표. 형식이 깨졌거나 값 범위를 벗어나면 null 을 돌려
     * 호출자가 코드 기본값으로 폴백하게 한다 (수동 편집/포맷 변경 대비).
     */
    private fun deserialize(raw: String): AccountDayCoordinateOverride.DayCoordinate? {
        // 라벨에 구분자가 섞여도 앞 3개만 끊어내고 나머지는 라벨로 취급한다.
        val parts = raw.split(DELIMITER, limit = 4)
        if (parts.size < 4) {
            log.warn("이동매장 좌표 예외 형식 오류 → 코드 기본값 폴백")
            return null
        }
        val day = AccountDayCoordinateOverride.dayOfWeekOrNull(parts[0])
        val lat = parts[1].toDoubleOrNull()
        val lng = parts[2].toDoubleOrNull()
        if (day == null || lat == null || lng == null || lat !in LAT_RANGE || lng !in LNG_RANGE) {
            log.warn("이동매장 좌표 예외 값 오류 → 코드 기본값 폴백")
            return null
        }
        return AccountDayCoordinateOverride.DayCoordinate(day, lat, lng, parts[3])
    }

    companion object {
        private const val COORDINATE_KEY =
            "account_day_coordinate:${AccountDayCoordinateOverride.TARGET_EXTERNAL_KEY}"
        private const val DELIMITER = "|"
        private val LAT_RANGE = -90.0..90.0
        private val LNG_RANGE = -180.0..180.0
    }
}
