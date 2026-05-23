package com.otoki.powersales.auth.permission

import com.otoki.powersales.auth.sharing.entity.PermissionSetFlags
import com.otoki.powersales.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

/**
 * SF 권한 모델 기반 user permission set 산출 (spec #801 + spec #808).
 *
 * 로그인 시점에 호출되어 user 의 SF Profile + PermissionSetAssignment 일람으로부터 평탄화된
 * permission key set 을 산출. JWT claim 으로 운반되어 WebAdminContextFilter 의 가드 검사에 사용.
 *
 * ## permission key 형식
 *
 * - entity × operation: `"<entity-table-name>:<R|C|E|D>"` (예: `"employee:R"`, `"account:E"`)
 * - custom resource × operation: 동일 형식 (예: `"dashboard:R"`) — JPA entity 가 없는 가상 자원
 * - system permission: `"SYSTEM:<SfSystemPermission>"` (예: `"SYSTEM:VIEW_ALL_DATA"`)
 *
 * ## 산출 로직
 *
 * 1. user.profileId → ProfileFlags 조회 → system permission 비트 5종 평가
 * 2. user.id → PermissionSetAssignment 일람 → 각 PermissionSetFlags 조회:
 *    - `object_permissions` JSON 파싱 (SF API name → entity table name 변환)
 *    - `custom_permissions` JSON 파싱 (spec #808 — 가상 자원, 변환 불요)
 *    - `view_all_data` / `modify_all_data` 비트도 system permission 으로 평가
 * 3. 모든 권한 key 합집합 반환
 *
 * ## VIEW_ALL_DATA / MODIFY_ALL_DATA 전파 (spec #808 일반화)
 *
 * - VIEW_ALL_DATA 비트 TRUE → **모든 자원 (entity + custom resource)** 의 READ 키 자동 포함
 * - MODIFY_ALL_DATA 비트 TRUE → **모든 자원** 의 모든 CRUD 키 자동 포함
 *
 * 본 펼침 정책으로 권한 가드 시점에는 단순 `permissions.contains(key)` 만 확인.
 */
@Service
class SfPermissionResolver(
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val permissionSetAssignmentRepository: PermissionSetAssignmentRepository,
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository,
    private val entitySfNameRegistry: EntitySfNameRegistry,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * user 의 SF 권한 key set 산출 — 평탄화 형식.
     */
    fun resolveForUser(user: User): Set<String> {
        val result = mutableSetOf<String>()

        // 1. ProfileFlags — system permission 비트
        val profileFlags: ProfileFlags? = user.profileId?.let { profileFlagsRepository.findByProfileId(it) }
        profileFlags?.let { applyProfileFlags(it, result) }

        // 2. PermissionSetAssignment 일람
        val assignments = permissionSetAssignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(user.id)
        for (assignment in assignments) {
            val psFlags: PermissionSetFlags = assignment.permissionSetFlagsId?.let {
                permissionSetFlagsRepository.findById(it).orElse(null)
            } ?: continue
            applyPermissionSetFlags(psFlags, result)
        }

        // 3. VIEW_ALL_DATA / MODIFY_ALL_DATA 펼침
        expandAllDataBits(result)

        return result
    }

    private fun applyProfileFlags(flags: ProfileFlags, result: MutableSet<String>) {
        if (flags.permissionsViewAllData) result.add(systemKey(SfSystemPermission.VIEW_ALL_DATA))
        if (flags.permissionsModifyAllData) result.add(systemKey(SfSystemPermission.MODIFY_ALL_DATA))
        if (flags.permissionsViewAllUsers) result.add(systemKey(SfSystemPermission.VIEW_ALL_USERS))
        if (flags.permissionsManageUsers) result.add(systemKey(SfSystemPermission.MANAGE_USERS))
        if (flags.permissionsApiEnabled) result.add(systemKey(SfSystemPermission.API_ENABLED))
    }

    private fun applyPermissionSetFlags(flags: PermissionSetFlags, result: MutableSet<String>) {
        if (flags.permissionsViewAllData) result.add(systemKey(SfSystemPermission.VIEW_ALL_DATA))
        if (flags.permissionsModifyAllData) result.add(systemKey(SfSystemPermission.MODIFY_ALL_DATA))

        applyObjectPermissionsJson(flags, result)
        applyCustomPermissionsJson(flags, result)
    }

    /**
     * `object_permissions` JSON — SF API name 키 → entity table name 으로 변환 후 권한 key 산출.
     */
    private fun applyObjectPermissionsJson(flags: PermissionSetFlags, result: MutableSet<String>) {
        val json = flags.objectPermissions?.takeIf { it.isNotBlank() } ?: return
        val parsed = try {
            objectMapper.readValue(json, Map::class.java) as? Map<*, *> ?: return
        } catch (e: Exception) {
            log.warn("[SfPermissionResolver] PermissionSetFlags id={} object_permissions JSON 파싱 실패: {}", flags.id, e.message)
            return
        }

        for ((sfApiName, perms) in parsed) {
            val sfApiNameStr = sfApiName as? String ?: continue
            val entityTableName = entitySfNameRegistry.toEntityTableName(sfApiNameStr) ?: continue
            val permsMap = perms as? Map<*, *> ?: continue
            addCrudKeys(entityTableName, permsMap, result)
        }
    }

    /**
     * `custom_permissions` JSON — 자원 이름 키 (JPA entity 없는 가상 자원) 그대로 권한 key 산출. (spec #808)
     */
    private fun applyCustomPermissionsJson(flags: PermissionSetFlags, result: MutableSet<String>) {
        val json = flags.customPermissions?.takeIf { it.isNotBlank() } ?: return
        val parsed = try {
            objectMapper.readValue(json, Map::class.java) as? Map<*, *> ?: return
        } catch (e: Exception) {
            log.warn("[SfPermissionResolver] PermissionSetFlags id={} custom_permissions JSON 파싱 실패: {}", flags.id, e.message)
            return
        }

        for ((resourceName, perms) in parsed) {
            val resourceNameStr = resourceName as? String ?: continue
            val permsMap = perms as? Map<*, *> ?: continue
            addCrudKeys(resourceNameStr, permsMap, result)
        }
    }

    private fun addCrudKeys(resourceName: String, permsMap: Map<*, *>, result: MutableSet<String>) {
        if (permsMap["allowRead"] == true) result.add(entityKey(resourceName, SfPermissionOperation.READ))
        if (permsMap["allowCreate"] == true) result.add(entityKey(resourceName, SfPermissionOperation.CREATE))
        if (permsMap["allowEdit"] == true) result.add(entityKey(resourceName, SfPermissionOperation.EDIT))
        if (permsMap["allowDelete"] == true) result.add(entityKey(resourceName, SfPermissionOperation.DELETE))
    }

    /**
     * VIEW_ALL_DATA → 모든 자원 READ 펼침. MODIFY_ALL_DATA → 모든 자원 CRUD 펼침. (spec #808 일반화)
     *
     * 펼침 대상은 [EntitySfNameRegistry.allResources] — `@SFObject` 부착 여부 무관 모든 JPA entity
     * + `@PermissionResource` 명시 등록 가상 자원 합집합.
     */
    private fun expandAllDataBits(result: MutableSet<String>) {
        val hasViewAll = result.contains(systemKey(SfSystemPermission.VIEW_ALL_DATA))
        val hasModifyAll = result.contains(systemKey(SfSystemPermission.MODIFY_ALL_DATA))
        if (!hasViewAll && !hasModifyAll) return

        for (resourceName in entitySfNameRegistry.allResources()) {
            if (hasViewAll || hasModifyAll) {
                result.add(entityKey(resourceName, SfPermissionOperation.READ))
            }
            if (hasModifyAll) {
                result.add(entityKey(resourceName, SfPermissionOperation.CREATE))
                result.add(entityKey(resourceName, SfPermissionOperation.EDIT))
                result.add(entityKey(resourceName, SfPermissionOperation.DELETE))
            }
        }
    }

    companion object {
        /**
         * entity × operation 의 평탄화 키. SfPermissionOperation 의 첫 글자로 압축.
         */
        fun entityKey(entityTableName: String, operation: SfPermissionOperation): String =
            "$entityTableName:${operation.shortCode()}"

        /**
         * system permission 의 평탄화 키.
         */
        fun systemKey(permission: SfSystemPermission): String = "SYSTEM:${permission.name}"

        private fun SfPermissionOperation.shortCode(): String = when (this) {
            SfPermissionOperation.READ -> "R"
            SfPermissionOperation.CREATE -> "C"
            SfPermissionOperation.EDIT -> "E"
            SfPermissionOperation.DELETE -> "D"
            SfPermissionOperation.SYSTEM -> "S"
        }
    }
}
