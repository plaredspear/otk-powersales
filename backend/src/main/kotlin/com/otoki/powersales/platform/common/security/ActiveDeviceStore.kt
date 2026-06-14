package com.otoki.powersales.platform.common.security

import com.otoki.powersales.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 활성 단말(active device) 캐시 + 검증.
 *
 * 단말 바인딩 모델(관리자 초기화)에서 "현재 로그인 허용된 기기"를 매 요청마다 확인해,
 * 관리자가 [com.otoki.powersales.platform.auth.service.AuthService.resetDevice] 로 단말을 회수하면
 * 기존 기기의 access token 을 즉시 무효화한다 (단말 교체 시 기존 기기 로그인 제한).
 *
 * 소스 오브 트루스는 DB `employee_info.device_uuid` 이며, Redis 는 성능용 read-through 캐시다.
 * - Redis 연결 실패 / 키 부재(miss) 는 **동일하게 DB 폴백** → 캐시 부재가 "차단 판정"이 되지 않는다.
 *   (부재를 차단으로 처리하면 Redis eviction 한 번에 전원 강제 로그아웃되는 가용성 사고가 난다.)
 * - 차단 여부는 항상 DB device_uuid 로 판정한다 (null = 회수됨 → 차단).
 */
@Component
class ActiveDeviceStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val employeeRepository: EmployeeRepository,
    @Value("\${app.auth.active-device-ttl:3600000}") private val ttlMillis: Long
) {
    private val log = LoggerFactory.getLogger(ActiveDeviceStore::class.java)

    /** 로그인/리프레시 시 현재 활성 기기를 캐시에 기록. Redis 장애는 무시(DB가 소스). */
    fun setActiveDevice(userId: Long, deviceId: String) {
        try {
            redisTemplate.opsForValue().set(key(userId), deviceId, Duration.ofMillis(ttlMillis))
        } catch (e: Exception) {
            log.warn("active_device 캐시 기록 실패(무시): userId={}", userId, e)
        }
    }

    /** 단말 초기화 시 활성 기기 캐시 제거. Redis 장애는 무시(DB가 소스). */
    fun clearActiveDevice(userId: Long) {
        try {
            redisTemplate.delete(key(userId))
        } catch (e: Exception) {
            log.warn("active_device 캐시 제거 실패(무시): userId={}", userId, e)
        }
    }

    /**
     * 토큰의 deviceId 가 현재 활성 기기와 일치하는지.
     *
     * Redis 우선 조회 → 연결 실패 또는 키 부재 시 DB(device_uuid) 폴백 후 재적재.
     * device_uuid == null(회수됨) 또는 불일치면 false → 호출부가 401(DEVICE_REVOKED) 처리.
     */
    fun isDeviceActive(userId: Long, tokenDeviceId: String): Boolean {
        val cached = try {
            redisTemplate.opsForValue().get(key(userId))
        } catch (e: Exception) {
            log.warn("active_device 캐시 조회 실패 → DB 폴백: userId={}", userId, e)
            null
        }
        if (cached != null) {
            return cached == tokenDeviceId
        }
        // 캐시 미스 / Redis 장애 → DB 가 소스 오브 트루스
        val dbDeviceId = employeeRepository.findWithEmployeeInfoById(userId)?.deviceUuid
            ?: return false // 바인딩 없음(=회수됨) → 차단
        setActiveDevice(userId, dbDeviceId) // 재적재 (Redis 정상 시에만 반영)
        return dbDeviceId == tokenDeviceId
    }

    private fun key(userId: Long) = "active_device:$userId"
}
