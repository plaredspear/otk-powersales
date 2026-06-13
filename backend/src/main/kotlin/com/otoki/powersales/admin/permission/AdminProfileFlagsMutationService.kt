package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.ProfileFlagsMutationResponse
import com.otoki.powersales.admin.permission.dto.ProfileUpdateFlagsRequest
import com.otoki.powersales.admin.permission.exception.InvalidCustomPermissionKeyException
import com.otoki.powersales.admin.permission.exception.InvalidObjectPermissionKeyException
import com.otoki.powersales.admin.permission.exception.ProfileNotFoundException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.platform.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.platform.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.platform.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.common.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Profile 권한 비트 (system 5종 + object + custom) 편집 service.
 *
 * ## 레거시 매핑
 * SF org 의 Profile 권한 편집은 SF Setup UI 가 수행 (Apex 코드 외). 본 service 는 신규 시스템 Web admin
 * 에서 동등 기능을 제공 — 발령(AppointmentTriggerHanlder) 이 직책 → ProfileId 를 자동 결정하므로,
 * Profile 권한 비트에 객체권한 (예: monthly_sales_history Read) 을 박아두면 해당 직책 사원에게 화면 권한이
 * 자동 전파된다 (SF 의 "직책 → Profile → 화면권한" 자동 연결고리 정합).
 *
 * 정책:
 * - 가드: MANAGE_USERS 시스템 권한 (controller @RequiresSfPermission)
 * - dirty 플래그 (`is_locally_modified`): 편집 시 항상 set. Stage2 ProfileFlags 재적재 시 dirty row 는
 *   skip 하여 신규 시스템 변경분을 SF retrieve 재적재로부터 보호 ([ProfileFlagsEvaluator] / Stage2 loader).
 *   PermissionSetFlags 의 동일 정책과 정합 — 단 Profile 은 신규 생성 기능이 없어 항상 SF 출처라
 *   조건 없이 set (PS 는 sfid != null 일 때만 set).
 * - upsert: ProfileFlags 행은 일부 Profile 에만 존재하므로 (로컬 '시스템 관리자' 1건 시드 / dev·prod 는
 *   SF systemPermissions 정의 Profile 만 Stage1 적재), 행이 없으면 기본값(전부 false) 신규 생성 후 편집분 교체
 * - 권한 비트 키 검증: EntitySfNameRegistry snapshot()/allResources() 기준 — 미등록 키 400
 * - 전체 교체 방식: 누락 키는 "권한 없음" 으로 해석 (부분 patch 아님)
 * - 캐시: 편집 후 CACHE_PERMISSION_MATRIX evict + CACHE_PROFILE_FLAGS (Profile 보유 user 전원 영향) clear
 *   + AdminPermissionCache / AdminDataScopeCache invalidateAll
 *
 * 변경 이력: 본 단계에서는 permission_set_change_log (PS 전용) 를 재사용하지 않고 로그만 남긴다.
 * Profile 변경 이력 audit 이 필요하면 후속에서 별도 테이블/이벤트로 분리.
 */
@Service
class AdminProfileFlagsMutationService(
    private val profileRepository: ProfileRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val entitySfNameRegistry: EntitySfNameRegistry,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache,
    private val cacheManager: CacheManager,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Profile 권한 비트 전체 교체.
     *
     * system 비트 5종 + object/custom 권한 JSON 을 교체하고 is_locally_modified set.
     * objectPermissions 키는 SF 매핑 entity (snapshot) 만, customPermissions 키는 가상 자원 (allResources - SF 매핑) 만 허용.
     * 편집 후 권한 매트릭스 / Profile flags / admin 권한·스코프 캐시 일괄 무효화.
     */
    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun updateFlags(
        profileId: Long,
        request: ProfileUpdateFlagsRequest,
        principalUserId: Long,
    ): ProfileFlagsMutationResponse {
        val profile = profileRepository.findById(profileId).orElseThrow {
            ProfileNotFoundException(profileId)
        }
        // ProfileFlags 행은 일부 Profile 에만 존재한다 (로컬은 '시스템 관리자' 1건만 시드,
        // dev/prod 는 SF systemPermissions 가 정의된 Profile 만 Stage1 적재). 행이 없는 Profile 도
        // 편집 가능해야 하므로 upsert — 없으면 기본값(전부 false) 신규 생성 후 편집분으로 교체.
        val flags = profileFlagsRepository.findByProfileId(profileId)
            ?: ProfileFlags(profileId = profileId)

        validateObjectPermissionKeys(request.objectPermissions)
        validateCustomPermissionKeys(request.customPermissions)

        flags.permissionsViewAllData = request.viewAllData
        flags.permissionsModifyAllData = request.modifyAllData
        flags.permissionsViewAllUsers = request.viewAllUsers
        flags.permissionsManageUsers = request.manageUsers
        flags.permissionsApiEnabled = request.apiEnabled
        flags.objectPermissions = objectMapper.writeValueAsString(request.objectPermissions)
        flags.customPermissions = objectMapper.writeValueAsString(request.customPermissions)
        flags.isLocallyModified = true
        profileFlagsRepository.save(flags)

        invalidateCaches()
        log.info(
            "[AdminProfileFlagsMutationService] updated flags profileId={} name={} principalUserId={}",
            profileId, profile.name, principalUserId,
        )

        return ProfileFlagsMutationResponse(
            profileId = profile.id,
            name = profile.name,
            viewAllData = flags.permissionsViewAllData,
            modifyAllData = flags.permissionsModifyAllData,
            viewAllUsers = flags.permissionsViewAllUsers,
            manageUsers = flags.permissionsManageUsers,
            apiEnabled = flags.permissionsApiEnabled,
            objectPermissions = parseJsonOrEmpty(flags.objectPermissions),
            customPermissions = parseJsonOrEmpty(flags.customPermissions),
            isLocallyModified = flags.isLocallyModified,
        )
    }

    private fun invalidateCaches() {
        // Profile 권한 변경은 해당 Profile 보유 user 전원의 권한 산출에 영향 — user 단위 evict 불가하여 clear.
        cacheManager.getCache(CacheConfig.CACHE_PROFILE_FLAGS)?.clear()
        adminPermissionCache.invalidateAll()
        adminDataScopeCache.invalidateAll()
    }

    private fun validateObjectPermissionKeys(map: Map<String, Map<String, Boolean>>) {
        val sfNames = entitySfNameRegistry.snapshot().values.toSet()
        for (key in map.keys) {
            if (key !in sfNames) throw InvalidObjectPermissionKeyException(key)
        }
    }

    private fun validateCustomPermissionKeys(map: Map<String, Map<String, Boolean>>) {
        val sfMappedEntities = entitySfNameRegistry.snapshot().keys
        val customResources = entitySfNameRegistry.allResources() - sfMappedEntities
        for (key in map.keys) {
            if (key !in customResources) throw InvalidCustomPermissionKeyException(key)
        }
    }

    private fun parseJsonOrEmpty(json: String?): Map<String, Map<String, Boolean>> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(json, Map::class.java) as Map<String, Map<String, Boolean>>
        } catch (e: Exception) {
            log.warn("[AdminProfileFlagsMutationService] permissions JSON 파싱 실패: {}", e.message)
            emptyMap()
        }
    }
}
