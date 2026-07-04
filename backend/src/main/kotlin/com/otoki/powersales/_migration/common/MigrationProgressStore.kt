package com.otoki.powersales._migration.common

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * 마이그레이션 진행 상태 스냅샷의 Redis 공유 저장소 (SF/Heroku × Stage1/Stage2 공통).
 *
 * 왜 Redis 인가:
 *  각 progress 빈은 원래 단일 backend 인스턴스의 in-memory 필드에만 상태를 담았다. Elastic Beanstalk
 *  다중 인스턴스 환경에서는 실행을 받은 인스턴스(A)와 `/progress` polling 을 받는 인스턴스(B)가 ALB
 *  라운드로빈으로 갈리면서, B 로 갈 때마다 IDLE 스냅샷이 응답되어 UI 에 "데이터 있음/없음"이 번갈아
 *  표시됐다. 진행 스냅샷을 Redis 단일 키에 두어 어느 인스턴스로 조회가 라우팅되어도 같은 값을 읽게 한다.
 *
 * 저장 규약:
 *  - 키: `migration:progress:<slug>` (예: `migration:progress:sf-stage2-fk`)
 *  - 값: 각 progress 의 Response DTO 를 그대로 JSON 직렬화한 스냅샷 1개. 워커 스레드가 mutate 할
 *    때마다 [save] 로 덮어쓴다 (매 chunk write — 폴링이 1초 간격이라 write 량 부담 적음).
 *  - TTL: 없음. 다음 실행의 begin() 이 스냅샷을 덮어쓰고, 마이그레이션은 1회성 cut-over 도구다.
 *
 * Redis 미사용 환경 (local/test): [redisTemplate] 이 null 이면 save/load 모두 no-op 이 되고, 호출자
 * (progress 빈)는 자신의 in-memory 필드로 폴백한다. 로컬은 단일 인스턴스라 in-memory 로 충분하다.
 *
 * 장애 정책: Redis 예외는 삼키고 로그만 남긴다. save 실패 시 in-memory 상태는 유지되므로 실행 자체는
 * 계속되고, load 실패 시 호출자가 in-memory 폴백으로 응답한다 (진행 표시가 없어질 뿐 데이터 유실 없음).
 */
@Component
class MigrationProgressStore(
    /** Redis 미사용 환경(test profile 등)에서는 빈 미등록 — null 허용. null 이면 save/load no-op. */
    private val redisTemplate: StringRedisTemplate?,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(MigrationProgressStore::class.java)

    /** Redis 스냅샷을 쓸 수 있는 환경인지 (다중 인스턴스 공유가 유효한지). */
    val enabled: Boolean get() = redisTemplate != null

    /** [snapshot] DTO 를 JSON 으로 직렬화해 Redis 에 덮어쓴다. Redis 미사용/장애 시 no-op. */
    fun save(slug: String, snapshot: Any) {
        val template = redisTemplate ?: return
        try {
            template.opsForValue().set(key(slug), objectMapper.writeValueAsString(snapshot))
        } catch (ex: Exception) {
            log.warn("[MIGRATION_PROGRESS] Redis 저장 실패 — in-memory 상태 유지. slug={} cause={}", slug, ex.message)
        }
    }

    /**
     * Redis 스냅샷을 [type] 으로 역직렬화해 반환. 키 부재 / Redis 미사용 / 장애 시 null (호출자 폴백).
     */
    fun <T> load(slug: String, type: Class<T>): T? {
        val template = redisTemplate ?: return null
        return try {
            template.opsForValue().get(key(slug))?.let { objectMapper.readValue(it, type) }
        } catch (ex: Exception) {
            log.warn("[MIGRATION_PROGRESS] Redis 조회 실패 — in-memory 폴백. slug={} cause={}", slug, ex.message)
            null
        }
    }

    private fun key(slug: String): String = "$KEY_PREFIX$slug"

    companion object {
        const val KEY_PREFIX: String = "migration:progress:"

        const val SLUG_SF_STAGE1: String = "sf-stage1-copy"
        const val SLUG_SF_STAGE2_FK: String = "sf-stage2-fk"
        const val SLUG_HEROKU_STAGE1: String = "heroku-stage1-copy"
        const val SLUG_HEROKU_STAGE2_FK: String = "heroku-stage2-fk"

        /**
         * Redis 없이 동작하는 no-op 저장소 — 단위 테스트에서 progress 빈을 직접 생성할 때 사용.
         * redisTemplate 이 null 이라 save/load 는 아무 것도 하지 않고, progress 는 in-memory 로만 동작한다.
         */
        fun noop(): MigrationProgressStore =
            MigrationProgressStore(redisTemplate = null, objectMapper = ObjectMapper())
    }
}
