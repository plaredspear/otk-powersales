package com.otoki.powersales.platform.auth.permission

import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.platform.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.platform.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.platform.common.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

/**
 * 조장 계열 Profile 의 ProfileFlags 초기값 부팅 sync.
 *
 * [LeaderProfileFlagsSeed] 의 declarative SoT 를 부팅 시 DB 에 반영한다 —
 * dev 에서 확정한 조장 권한을 모든 환경(dev / prod)에 재현. CLAUDE.md §4 reference data 정책
 * (Flyway INSERT 금지 / SoT + 부팅 sync) 정합. [ProdAdminBootstrapInitializer] 의 부팅 시드 스타일을 따른다.
 *
 * ## 적용 규칙 (초기값-only)
 * 대상 Profile 의 ProfileFlags row 가 **없거나 `is_locally_modified=FALSE` 일 때만** SoT 값으로 upsert.
 * web admin 편집분(`is_locally_modified=TRUE`)은 skip 하여 운영 편집 자율성 + SF 재적재 dirty-skip 정책과 정합.
 * upsert 로 넣은 row 는 `is_locally_modified=FALSE` 를 유지 → 이후 web admin 편집 시 보호 대상으로 전환.
 *
 * ## 순서 의존
 * profile.name 자연 키로 lookup 하므로 profile row 가 먼저 존재해야 한다. dev/prod 는 SF Stage1 Profile
 * 적재 (12종) 후 존재, local 은 LocalDataInitializer.seedProfiles 가 보장. profile 부재 시 해당 seed 만 skip
 * (부팅 순서상 아직 미적재일 수 있어 warn 만 남기고 다음 부팅 또는 마이그레이션 후 재기동 시 반영).
 *
 * ## JSON 정규화
 * SoT 원문(가독성 위해 공백 포함)을 파싱 후 compact 재직렬화하여 저장 — DB objectPermissions 형식
 * (AdminProfileFlagsMutationService 저장분)과 정합. dev/local 은 dev(dev|prod)에서만 실행하지 않고
 * SF 마이그레이션 대상 환경 전반에 필요하므로 `dev | prod` 프로파일.
 */
@Component
@Profile("dev | prod")
class LeaderProfileFlagsSyncRunner(
    private val profileRepository: ProfileRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache,
    private val cacheManager: CacheManager,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        try {
            val applied = transactionTemplate.execute { syncLeaderProfileFlags() } ?: 0
            if (applied > 0) invalidateCaches()
        } catch (e: Exception) {
            log.warn("조장 ProfileFlags 초기값 sync 실행 실패 (기존 데이터 충돌 가능): {}", e.message)
        }
    }

    /** @return 실제 적용(신규 생성 또는 갱신)한 seed 수. dirty/profile 부재로 skip 한 건은 제외. */
    private fun syncLeaderProfileFlags(): Int {
        var applied = 0
        for (seed in LeaderProfileFlagsSeed.SEEDS) {
            val profile = profileRepository.findByName(seed.profileName)
            if (profile == null) {
                log.warn("조장 ProfileFlags seed skip — profile 미적재: name={}", seed.profileName)
                continue
            }
            val existing = profileFlagsRepository.findByProfileId(profile.id)
            if (existing != null && existing.isLocallyModified) {
                log.info(
                    "조장 ProfileFlags seed skip — dirty(web admin 편집분): name={} profileId={}",
                    seed.profileName, profile.id,
                )
                continue
            }
            val flags = existing ?: ProfileFlags(profileId = profile.id)
            flags.permissionsViewAllData = seed.viewAllData
            flags.permissionsModifyAllData = seed.modifyAllData
            flags.permissionsViewAllUsers = seed.viewAllUsers
            flags.permissionsManageUsers = seed.manageUsers
            flags.permissionsApiEnabled = seed.apiEnabled
            flags.objectPermissions = compact(seed.objectPermissionsJson)
            flags.customPermissions = seed.customPermissionsJson?.let { compact(it) }
            flags.isLocallyModified = false
            profileFlagsRepository.save(flags)
            applied++
            log.info(
                "조장 ProfileFlags seed 적용: name={} profileId={} action={}",
                seed.profileName, profile.id, if (existing == null) "create" else "update",
            )
        }
        return applied
    }

    /** SoT 원문 JSON → compact 재직렬화 (DB 저장 형식 정합). */
    private fun compact(json: String): String =
        objectMapper.writeValueAsString(objectMapper.readTree(json))

    private fun invalidateCaches() {
        cacheManager.getCache(CacheConfig.CACHE_PROFILE_FLAGS)?.clear()
        cacheManager.getCache(CacheConfig.CACHE_PERMISSION_MATRIX)?.clear()
        adminPermissionCache.invalidateAll()
        adminDataScopeCache.invalidateAll()
    }
}
