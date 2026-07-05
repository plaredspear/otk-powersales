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
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper

/**
 * 조장 계열 Profile 의 ProfileFlags 초기값 부팅 sync — **현재 비활성화 (`@Component` 미부착)**.
 *
 * ## 비활성화 사유 (SF 데이터 마이그레이션 SF값 완전 우선)
 * 본 Runner 는 부팅 시 조장 계열 Profile("6.조장" / "7.영업사원 + 조장") 의 ProfileFlags row 를
 * `findByProfileId` 로 찾아 없으면 **새로 create** 했다. 그런데 SF Stage1 은 같은 profile 을
 * (profile_name=존재, profile_id=NULL) 별도 row 로 적재하고, Runner 는 profile_id 로만 조회하므로
 * Stage1 row 를 못 찾아 (profile_name=NULL, profile_id=존재) row 를 별도 생성했다. profile_flags 는
 * profile_name / profile_id 각각 UNIQUE 라 두 row 가 공존 → Stage2 FK Resolve 가 Stage1 row 의
 * profile_id 를 채우려는 순간 `profile_flags_profile_id_key` UNIQUE 위반 (운영 관측: profile_id=25 already exists).
 *
 * 결정: **SF 추출값을 조장 profile 에도 완전 우선** — Runner 를 비활성화해 부팅 시 profile_flags 를
 * create/update 하지 않는다. profile_flags row 는 Stage1 SF row 하나만 남고, Stage2 FK Resolve 의
 * 단순 UPDATE(NATURAL_KEY_FK_MAPPINGS 의 profile_flags 매핑)로 profile_id 만 채워진다.
 * 조장 권한은 SF 원본 object_permissions 를 그대로 사용한다.
 *
 * ## 되살릴 때
 * 조장 권한을 다시 코드 SoT 로 관리해야 하면 `@Component` + `@Profile("dev | prod")` 재부착 + 반드시
 * SF 마이그레이션(Stage2 FK Resolve) 이후에만 실행되도록 순서를 보장하고(그때 profile_id 가 채워져 있어
 * create 분기가 타지 않음), create 분기를 제거해 "존재 시 update, 없으면 skip" 으로 바꿔야 UNIQUE 충돌이
 * 재발하지 않는다. [LeaderProfileFlagsSeed] 의 SoT 정의는 그대로 보존한다.
 *
 * ## JSON 정규화 (참고 — 되살릴 때 유효)
 * SoT 원문(가독성 위해 공백 포함)을 파싱 후 compact 재직렬화하여 저장 — DB objectPermissions 형식
 * (AdminProfileFlagsMutationService 저장분)과 정합.
 */
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
