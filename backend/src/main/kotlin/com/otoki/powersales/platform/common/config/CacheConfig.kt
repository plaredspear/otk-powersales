package com.otoki.powersales.platform.common.config

import org.slf4j.LoggerFactory
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
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
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
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
 * [com.otoki.powersales.domain.org.organization.service.OrganizationReplaceService.replaceAll] 의
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
 * `GenericJacksonJsonRedisSerializer` 의 default typing 은 builder 의 [enableDefaultTyping] 으로 명시
 * 활성화해야 한다. 미활성화 시 `List<DTO>` 역직렬화가 LinkedHashMap 으로 떨어져 캐시 hit 후
 * ConversionFailedException 이 발생한다. typeValidator 는 `Object.class` 기반 permissive 설정 —
 * 캐시 데이터 출처가 application 내부 SoT 라 외부 신뢰 경계 문제 없음. Spring Cache 의 `NullValue`
 * marker 역직렬화도 default typing 활성화 시 자동 지원. DTO 가 `Serializable` 을 구현하지 않아도
 * JSON 직렬화 대상이므로 캐시 가능.
 */
@Configuration
@EnableCaching
class CacheConfig : CachingConfigurer {

    companion object {
        private val log = LoggerFactory.getLogger(CacheConfig::class.java)

        /**
         * Organization cascade lookup 결과 (Organization 단건) — 24h TTL.
         *
         * v2 접미사: 이전 cache name (`organizationCascade`) 은 default typing 미활성화 상태로 LinkedHashMap
         * 으로 저장되어 새 직렬화 설정 (default typing on) 과 호환 불가. 운영 Redis flush 없이 자연 분리
         * (기존 entry 는 24h TTL 후 만료).
         */
        const val CACHE_ORGANIZATION_CASCADE = "organizationCascadeV2"

        /**
         * 여사원 일정관리 사업소 옵션 (List<BranchResponse>) — 24h TTL.
         *
         * v2 접미사 사유는 [CACHE_ORGANIZATION_CASCADE] 와 동일.
         */
        const val CACHE_TEAM_SCHEDULE_BRANCHES = "teamScheduleBranchesV2"

        /**
         * SF Sharing Rule 정책 evaluator cache name (spec #782 P2-B).
         *
         * 모두 1h TTL — 정책 변경 빈도 매우 낮음 (사원당 1~2년/회). UserRole entity / PermissionSetAssignment
         * 변경 시 @CacheEvict 로 즉시 invalidate.
         */
        const val CACHE_HIERARCHY_SUBORDINATES = "hierarchySubordinates"
        const val CACHE_HIERARCHY_ANCESTOR_PATH = "hierarchyAncestorPath"
        const val CACHE_MEMBER_GROUP_IDS = "memberGroupIds"
        const val CACHE_PROFILE_FLAGS = "profileFlags"
        const val CACHE_PERMISSION_SET_FLAGS = "permissionSetFlags"

        /**
         * SharingPolicyQueryRepository.findRulesForUser 결과 (User 별 매칭 sharing_rule subset).
         *
         * 1h TTL — 다른 sharing 캐시와 동일 정합. 매 admin 요청마다 `sharing_rule + sharing_rule_target`
         * JOIN + `sharing_rule_condition` IN-list 2 query 가 모두 캐시 hit 으로 1회 절약.
         *
         * 본 캐시는 입력으로 `ancestorPath` + `groupMemberships` 를 받지만 둘 다 다른 cache 의 산출물
         * (1h TTL 동일). 두 산출물이 변경되는 시점에 [SharingRecalcService.recalcAll] 일괄 evict 가
         * 본 캐시도 함께 무효화 — 일관성 유지.
         */
        const val CACHE_SHARING_RULES_FOR_USER = "sharing-rules-for-user:v1"

        /**
         * spec #791 — SF OWD + master-detail relationship cache.
         * spec #794 — Record Type 권한 cache.
         * spec #795 — FLS field permission cache.
         *
         * #792 의 sharing recalc admin endpoint 가 본 cache name 들도 일괄 evict.
         */
        const val CACHE_SOBJECT_SETTING = "sobject-setting:v2"
        const val CACHE_RECORD_TYPE_VISIBILITY = "record-type-visibility:v1"
        const val CACHE_FIELD_PERMISSION = "field-permission:v1"

        /** Spec #803 — entity × Profile 매트릭스 계산 결과 (5분 TTL). */
        const val CACHE_PERMISSION_MATRIX = "permission-matrix:v1"

        /**
         * 관리자 권한 가드용 평탄화 권한 key set (`Set<String>`) — 5분 TTL.
         *
         * 이전 Caffeine in-memory 구현은 멀티 인스턴스 환경에서 invalidate 가 호출된 단일 JVM 만
         * 비워 권한 stale 을 유발 → Redis 공유 캐시로 전환. WebAdminContextFilter 가 매 admin 요청마다
         * lookup. SF Profile / PermissionSetAssignment 변경 시 [AdminPermissionCache.invalidate] evict.
         */
        const val CACHE_ADMIN_PERMISSION = "admin-permission:v1"

        /**
         * 관리자 DataScope (조회 범위) — 5분 TTL.
         *
         * [CACHE_ADMIN_PERMISSION] 과 동일 사유로 Caffeine → Redis 전환. key = userId.
         *
         * v2 접미사: 근본 원인은 value serializer 의 ObjectMapper 에 KotlinModule 미등록이었다
         * ([defaultRedisCacheConfiguration] 참조). 미등록 상태에서 [com.otoki.powersales.admin.dto.DataScope]
         * 의 `is`-prefixed primitive Boolean `isAllBranches` 가 JSON 에 `allBranches` 로 기록되는데
         * 역직렬화는 생성자 파라미터 `isAllBranches` 를 찾아 absent → `FAIL_ON_NULL_FOR_PRIMITIVES` 로
         * `Cannot map null into type boolean` 예외 → admin API 500. KotlinModule 등록으로 직렬화 포맷이
         * `isAllBranches` 로 바뀌므로, cache name 을 bump 해 옛 `allBranches` 포맷 entry 와 키 공간을
         * 분리한다 — rolling 배포로 신·구 인스턴스가 공존해도 충돌 없고 옛 entry 는 5분 TTL 후 자연 소멸.
         */
        const val CACHE_ADMIN_DATA_SCOPE = "admin-data-scope:v2"

        /**
         * 모바일 버전 체크 메타 (플랫폼별 최신 버전 + 강제 업데이트 경계) — 5분 TTL.
         *
         * 로그인 전 스플래시에서 매 실행 호출되는 무인증 엔드포인트라 DB 조회(최신 버전 + force_update
         * 경계 2~3 query)를 캐시로 절약한다. key = platform. 앱 패키지 변경(upload/setLatest/
         * setForceUpdate/updateReleaseNote/delete) 시 @CacheEvict 로 즉시 무효화하며, 5분 TTL 은
         * evict 누락 / 다른 경로 적재의 fallback (최대 5분 내 자동 회복).
         */
        const val CACHE_APP_VERSION_META = "app-version-meta:v1"

        /** spec #792 — sharing recalc 가 일괄 evict 하는 cache name 일람 */
        val SHARING_RELATED_CACHE_NAMES: List<String> = listOf(
            CACHE_HIERARCHY_SUBORDINATES,
            CACHE_HIERARCHY_ANCESTOR_PATH,
            CACHE_MEMBER_GROUP_IDS,
            CACHE_PROFILE_FLAGS,
            CACHE_PERMISSION_SET_FLAGS,
            CACHE_SOBJECT_SETTING,
            CACHE_RECORD_TYPE_VISIBILITY,
            CACHE_FIELD_PERMISSION,
            CACHE_SHARING_RULES_FOR_USER,
        )

        private val ORGANIZATION_TTL: Duration = Duration.ofHours(24)
        private val SHARING_POLICY_TTL: Duration = Duration.ofHours(1)
        private val PERMISSION_MATRIX_TTL: Duration = Duration.ofMinutes(5)
        private val APP_VERSION_META_TTL: Duration = Duration.ofMinutes(5)
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
        // default typing 활성화 + Spring Cache NullValue 지원. typeValidator 는 builder 가 기본 제공하는
        // permissive 설정 (Object.class 기반) 과 동일 — 캐시 데이터 출처가 application 내부 SoT 라 안전.
        val typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Any::class.java)
            .allowIfSubType { _, _ -> true }
            .build()
        // KotlinModule 명시 등록 — builder 의 default ObjectMapper 는 KotlinModule 을 ServiceLoader 로
        // 자동 등록하지 않는다. 미등록 시 Kotlin data class 의 `is`-prefixed Boolean (예: DataScope.isAllBranches)
        // getter 가 Java Bean 규칙으로 `allBranches` property 로 직렬화되는 반면, 역직렬화는 생성자 파라미터명
        // `isAllBranches` 를 찾아 absent → primitive Boolean 에 null 매핑 (`Cannot map null into type boolean`)
        // 으로 admin API 500. KotlinModule 이 생성자 파라미터명을 property 이름으로 고정해 round-trip 정합.
        val valueSerializer: RedisSerializer<Any> = GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator)
            .enableSpringCacheNullValueSupport()
            .customize { it.addModule(KotlinModule.Builder().build()) }
            .build()

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
        val sharingPolicyConfig = defaultConfig.entryTtl(SHARING_POLICY_TTL)
        val permissionMatrixConfig = defaultConfig.entryTtl(PERMISSION_MATRIX_TTL)
        val appVersionMetaConfig = defaultConfig.entryTtl(APP_VERSION_META_TTL)
        val perCacheConfig = mapOf(
            CACHE_ORGANIZATION_CASCADE to defaultConfig,
            CACHE_TEAM_SCHEDULE_BRANCHES to defaultConfig,
            CACHE_HIERARCHY_SUBORDINATES to sharingPolicyConfig,
            CACHE_HIERARCHY_ANCESTOR_PATH to sharingPolicyConfig,
            CACHE_MEMBER_GROUP_IDS to sharingPolicyConfig,
            CACHE_PROFILE_FLAGS to sharingPolicyConfig,
            CACHE_PERMISSION_SET_FLAGS to sharingPolicyConfig,
            CACHE_SHARING_RULES_FOR_USER to sharingPolicyConfig,
            // spec #791 / #794 / #795 — sharing 관련 cache
            CACHE_SOBJECT_SETTING to sharingPolicyConfig,
            CACHE_RECORD_TYPE_VISIBILITY to sharingPolicyConfig,
            CACHE_FIELD_PERMISSION to sharingPolicyConfig,
            // spec #803 — 권한 매트릭스
            CACHE_PERMISSION_MATRIX to permissionMatrixConfig,
            // 관리자 권한 가드 캐시 (Caffeine → Redis 전환) — 5분 TTL
            CACHE_ADMIN_PERMISSION to permissionMatrixConfig,
            CACHE_ADMIN_DATA_SCOPE to permissionMatrixConfig,
            // 모바일 버전 체크 메타 — 5분 TTL
            CACHE_APP_VERSION_META to appVersionMetaConfig,
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

    /**
     * 캐시 작업 실패(주로 Redis 연결 불가) 시 예외를 전파하지 않고 삼켜서 **원본 메서드(DB 조회)로
     * graceful 하게 fallback** 하게 한다.
     *
     * Spring Cache 의 기본 동작은 캐시 get/put/evict 중 발생한 예외를 그대로 호출자에게 전파한다 —
     * 즉 Redis 가 죽으면 `@Cacheable` 메서드 호출 자체가 실패한다. 본 핸들러는 get/put/evict/clear
     * 실패를 WARN 로그만 남기고 무시하므로:
     *  - GET 실패 → 캐시 미스로 간주되어 Spring 이 원본 메서드를 호출(DB 조회) 후 정상 반환
     *  - PUT/EVICT/CLEAR 실패 → 무시 (다음 TTL 만료 또는 Redis 회복 시 자연 정합)
     *
     * 캐시는 성능 최적화일 뿐 정합성의 SoT 가 아니므로(SoT 는 DB), Redis 장애가 기능 장애로
     * 번지지 않게 하는 것이 옳다. 단 PUT/EVICT 실패는 stale 가능성이 있어 WARN 으로 가시화한다.
     */
    override fun errorHandler(): CacheErrorHandler = object : CacheErrorHandler {
        override fun handleCacheGetError(exception: RuntimeException, cache: Cache, key: Any) {
            log.warn("캐시 GET 실패 — 원본(DB) 조회로 fallback. cache={}, key={}", cache.name, key, exception)
        }

        override fun handleCachePutError(exception: RuntimeException, cache: Cache, key: Any, value: Any?) {
            log.warn("캐시 PUT 실패 — 무시(다음 회복 시 정합). cache={}, key={}", cache.name, key, exception)
        }

        override fun handleCacheEvictError(exception: RuntimeException, cache: Cache, key: Any) {
            log.warn("캐시 EVICT 실패 — 무시(TTL 만료로 자연 정합). cache={}, key={}", cache.name, key, exception)
        }

        override fun handleCacheClearError(exception: RuntimeException, cache: Cache) {
            log.warn("캐시 CLEAR 실패 — 무시. cache={}", cache.name, exception)
        }
    }
}
