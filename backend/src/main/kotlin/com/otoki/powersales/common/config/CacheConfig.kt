package com.otoki.powersales.common.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/**
 * Spring Cache 인프라 (PR 1).
 *
 * `@EnableCaching` 활성화 + cache name 별 TTL 정의.
 *
 * ## Profile 별 CacheManager
 *
 * | Profile        | CacheManager        | 동작                                                    |
 * |----------------|---------------------|---------------------------------------------------------|
 * | dev / prod     | [RedisCacheManager] | Redis 에 실제 캐싱                                       |
 * | test           | [NoOpCacheManager]  | 캐시 어노테이션 통과만 시키고 매번 원본 메서드 호출       |
 * | local          | [NoOpCacheManager]  | Redis 실 가동 부재 시 connection error 회피 (stub 보호)  |
 *
 * `@ConditionalOnBean(RedisConnectionFactory)` 만으로는 local profile 의 [LocalRedisStubConfig]
 * stub ConnectionFactory 가 빈을 만들어버려 캐시 호출이 connection error 로 fail 한다.
 * 따라서 NoOp 분기는 profile 기준으로 명시 (test, local).
 *
 * ## Cache 등록 컨벤션
 *
 * 신규 캐시 추가 시:
 *  1. 본 클래스의 cache name 상수 추가
 *  2. [redisCacheManager] 의 `perCacheConfig` Map 에 `cacheName → TTL` 항목 추가
 *  3. 서비스/리포지토리 메서드에 `@Cacheable(value = "<cacheName>", key = "...")` 부착
 *  4. 무효화 트리거 위치에 `@CacheEvict(value = ["<cacheName>"], allEntries = true)` 부착
 *
 * cache name 명명: 도메인 + 데이터 종류 (예: `organizationCascade`, `teamScheduleBranches`).
 *
 * ## TTL 정책
 *
 * Organization 캐시군 — 24h. SAP daily sync 가 새벽 시간대 1회 적재되며, 적재 직후
 * [com.otoki.powersales.organization.service.OrganizationReplaceService.replaceAll] 의
 * `@CacheEvict` 가 즉시 무효화한다. 24h TTL 은 evict 누락 / 다른 경로 적재 / 수동 수정 케이스의
 * fallback (다음날 자동 회복) 안전장치.
 *
 * ## 직렬화
 *
 * - Key: [StringRedisSerializer] — Spring Cache 기본 key 생성 규칙 (`SimpleKeyGenerator`) 의
 *   `toString()` 결과를 그대로 String 으로 직렬화.
 * - Value: [GenericJacksonJsonRedisSerializer] — Jackson 3 기반 (spring-data-redis 4.0 의 신규 표준).
 *   기존 `GenericJackson2JsonRedisSerializer` (Jackson 2 기반) 는 spring-data-redis 4.0 부터
 *   deprecated. 본 프로젝트는 application 본업이 Jackson 3 (`tools.jackson.*`) 이라 정합 양호.
 *
 * `GenericJacksonJsonRedisSerializer` 는 default typing 을 활성화하여 polymorphic 타입과
 * `List<DTO>` 같은 generic collection 도 역직렬화 시 타입 복원이 가능하다.
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        /** Organization cascade lookup 결과 (Organization 단건) — 24h TTL */
        const val CACHE_ORGANIZATION_CASCADE = "organizationCascade"

        /** 여사원 일정관리 사업소 옵션 (List<BranchResponse>) — 24h TTL */
        const val CACHE_TEAM_SCHEDULE_BRANCHES = "teamScheduleBranches"

        private val ORGANIZATION_TTL: Duration = Duration.ofHours(24)
    }

    /**
     * RedisCacheManager — Redis 가 실제 가동되는 환경 (dev / prod) 전용.
     *
     * test / local profile 은 [noOpCacheManager] 가 우선되어 본 빈은 생성되어도 사용되지 않는다
     * (둘 다 `@Primary` 지만 profile 분기로 한 시점에 1개만 활성).
     */
    @Bean
    @Primary
    @ConditionalOnBean(RedisConnectionFactory::class)
    @Profile("!test & !local")
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        // Jackson 3 ObjectMapper — application 본업과 분리된 캐시 전용 인스턴스. Kotlin module +
        // Java time 등은 findAndAddModules() 가 자동 등록.
        val cacheObjectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()
        val valueSerializer: RedisSerializer<Any> = GenericJacksonJsonRedisSerializer(cacheObjectMapper)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(ORGANIZATION_TTL)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
            )

        // cache name 별 TTL — 현재는 모두 ORGANIZATION_TTL 동일. 도메인 추가 시 본 Map 에 별도 entry.
        val perCacheConfig = mapOf(
            CACHE_ORGANIZATION_CASCADE to defaultConfig,
            CACHE_TEAM_SCHEDULE_BRANCHES to defaultConfig,
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(perCacheConfig)
            .build()
    }

    /**
     * Test / local profile fallback — NoOpCacheManager.
     *
     * test profile: Redis auto-config 가 제외되어 RedisCacheManager 미생성.
     * local profile: [LocalRedisStubConfig] 의 stub ConnectionFactory 가 실제 Redis 미가동 시
     * connection error 를 유발하므로 NoOp 으로 명시적 분기.
     *
     * NoOpCacheManager 는 `@Cacheable` / `@CacheEvict` 어노테이션을 통과만 시키고 매번 원본
     * 메서드를 호출한다. 즉 캐시 어노테이션이 부착된 메서드의 동작 시맨틱은 캐시 없음 케이스와 동등.
     */
    @Bean
    @Primary
    @Profile("test | local")
    fun noOpCacheManager(): CacheManager = NoOpCacheManager()
}
