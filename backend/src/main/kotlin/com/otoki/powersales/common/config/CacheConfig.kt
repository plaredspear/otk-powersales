package com.otoki.powersales.common.config

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

/**
 * Spring Cache 인프라.
 *
 * `@EnableCaching` 활성화 + cache name 별 TTL 정의.
 *
 * ## 통합 전략 — Spring Boot 자동 구성에 합류
 *
 * `RedisCacheManager` 빈을 직접 생성하지 않고 Spring Boot 의 [RedisCacheAutoConfiguration] 흐름에 합류한다.
 *
 * - [defaultRedisCacheConfiguration]: default 직렬화 / TTL 정의 — Spring Boot 가 이 빈을 발견하면 자동 구성에 사용
 * - [perCacheTtlCustomizer]: per-cache TTL / 등록 — `RedisCacheManagerBuilderCustomizer` 로 builder 에 합류
 *
 * 이전 구현은 `@Configuration` 에서 `RedisCacheManager` 빈을 직접 만들고 `@ConditionalOnBean(RedisConnectionFactory::class)`
 * 로 보호했으나, `RedisConnectionFactory` 자체가 [RedisAutoConfiguration] 의 조건부 빈이라 평가 순서에 따라 `@ConditionalOnBean`
 * 이 false 로 평가되어 빈이 등록되지 않고 Spring Boot 의 default `RedisCacheManager` (JdkSerializationRedisSerializer)
 * 가 fallback 되는 함정이 있었다 (운영에서 `List<BranchResponse>` 캐시 시 `NotSerializableException` 발생).
 *
 * 본 패턴은 자동 구성 흐름에 customize 만 합류시키므로 평가 순서 문제가 발생하지 않는다.
 *
 * ## Profile 별 CacheManager
 *
 * | Profile        | CacheManager                              | 동작                                                    |
 * |----------------|-------------------------------------------|---------------------------------------------------------|
 * | dev / prod     | Spring Boot 자동 구성 [RedisCacheManager]  | 본 클래스의 default config + per-cache customizer 적용     |
 * | test           | [NoOpCacheManager] (`@Primary`)           | 캐시 어노테이션 통과만 시키고 매번 원본 메서드 호출       |
 * | local          | [NoOpCacheManager] (`@Primary`)           | Redis 실 가동 부재 시 connection error 회피 (stub 보호)  |
 *
 * test / local profile 은 `@Primary` NoOpCacheManager 가 우선되어 자동 구성된 RedisCacheManager 가 있어도 사용되지 않는다.
 *
 * ## Cache 등록 컨벤션
 *
 * 신규 캐시 추가 시:
 *  1. 본 클래스의 cache name 상수 추가
 *  2. [perCacheTtlCustomizer] 의 `withCacheConfiguration` 호출 추가 (`cacheName → TTL`)
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
 * `List<DTO>` 같은 generic collection 도 역직렬화 시 타입 복원이 가능하다. DTO 가 `Serializable` 을
 * 구현하지 않아도 JSON 직렬화 대상이므로 캐시 가능.
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
     * Default [RedisCacheConfiguration] — Spring Boot 자동 구성이 이 빈을 발견하면 RedisCacheManager 의 default 로 사용.
     *
     * 본 빈이 등록된 상태에서 별도 cache name 의 `@Cacheable` 이 사용되면 (per-cache 미정의), 이 default 가 적용된다.
     * per-cache 별 별도 TTL/직렬화는 [perCacheTtlCustomizer] 에서 정의.
     */
    @Bean
    @Profile("!test & !local")
    fun defaultRedisCacheConfiguration(): RedisCacheConfiguration {
        // Jackson 3 ObjectMapper — application 본업과 분리된 캐시 전용 인스턴스. Kotlin module +
        // Java time 등은 findAndAddModules() 가 자동 등록.
        val cacheObjectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()
        val valueSerializer: RedisSerializer<Any> = GenericJacksonJsonRedisSerializer(cacheObjectMapper)

        return RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(ORGANIZATION_TTL)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
            )
    }

    /**
     * per-cache TTL 정의 — Spring Boot 가 RedisCacheManager 를 빌드할 때 호출하여 builder 에 합류.
     *
     * 현재는 모든 캐시가 [ORGANIZATION_TTL] (24h) 동일. 도메인 추가 시 본 customizer 에 별도 entry 추가.
     * `withInitialCacheConfigurations` 로 cache name 을 미리 등록해 두면, 호출 site 에서 `@Cacheable` 만 부착해도
     * lazy 생성 없이 사전 정의된 설정으로 캐시가 동작한다.
     */
    @Bean
    @Profile("!test & !local")
    fun perCacheTtlCustomizer(defaultConfig: RedisCacheConfiguration): RedisCacheManagerBuilderCustomizer {
        val perCacheConfig = mapOf(
            CACHE_ORGANIZATION_CASCADE to defaultConfig,
            CACHE_TEAM_SCHEDULE_BRANCHES to defaultConfig,
        )
        return RedisCacheManagerBuilderCustomizer { builder ->
            builder.withInitialCacheConfigurations(perCacheConfig)
        }
    }

    /**
     * Test / local profile fallback — NoOpCacheManager.
     *
     * test profile: Redis auto-config 가 제외되어 RedisCacheManager 미생성.
     * local profile: [LocalRedisStubConfig] 의 stub ConnectionFactory 가 실제 Redis 미가동 시
     * connection error 를 유발하므로 NoOp 으로 명시적 분기.
     *
     * `@Primary` 로 자동 구성된 RedisCacheManager 보다 우선되어 캐시 호출이 모두 본 NoOp 으로 흐른다.
     * NoOpCacheManager 는 `@Cacheable` / `@CacheEvict` 어노테이션을 통과만 시키고 매번 원본 메서드를 호출한다.
     */
    @Bean
    @Primary
    @Profile("test | local")
    fun noOpCacheManager(): CacheManager = NoOpCacheManager()
}
