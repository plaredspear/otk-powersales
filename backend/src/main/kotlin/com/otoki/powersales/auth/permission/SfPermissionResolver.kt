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
 * SF 권한 모델 기반 user permission set 산출 (spec #801).
 *
 * 로그인 시점에 호출되어 user 의 SF Profile + PermissionSetAssignment 일람으로부터 평탄화된
 * permission key set 을 산출. JWT claim 으로 운반되어 WebAdminContextFilter 의 가드 검사에 사용.
 *
 * ## permission key 형식
 *
 * - entity × operation: `"<entity-table-name>:<R|C|E|D>"` (예: `"employee:R"`, `"account:E"`)
 * - system permission: `"SYSTEM:<SfSystemPermission>"` (예: `"SYSTEM:VIEW_ALL_DATA"`)
 *
 * ## 산출 로직
 *
 * 1. user.profileId → ProfileFlags 조회 → system permission 비트 5종 평가
 * 2. user.id → PermissionSetAssignment 일람 → 각 PermissionSetFlags 조회 → object_permissions JSON 파싱
 * 3. PermissionSetFlags 의 view_all_data / modify_all_data 비트 (PS 측)도 system permission 으로 평가
 * 4. object_permissions JSON 의 (SF API name → CRUD bit) 를 EntitySfNameRegistry 로 (entity → CRUD) 변환
 * 5. 모든 권한 key 합집합 반환
 *
 * ## VIEW_ALL_DATA / MODIFY_ALL_DATA 전파
 *
 * - VIEW_ALL_DATA 비트 TRUE → 모든 entity 의 READ 키 자동 포함
 * - MODIFY_ALL_DATA 비트 TRUE → 모든 entity 의 모든 CRUD 키 자동 포함
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
            if (permsMap["allowRead"] == true) result.add(entityKey(entityTableName, SfPermissionOperation.READ))
            if (permsMap["allowCreate"] == true) result.add(entityKey(entityTableName, SfPermissionOperation.CREATE))
            if (permsMap["allowEdit"] == true) result.add(entityKey(entityTableName, SfPermissionOperation.EDIT))
            if (permsMap["allowDelete"] == true) result.add(entityKey(entityTableName, SfPermissionOperation.DELETE))
        }
    }

    /**
     * VIEW_ALL_DATA → 모든 entity READ 펼침. MODIFY_ALL_DATA → 모든 entity CRUD 펼침.
     */
    private fun expandAllDataBits(result: MutableSet<String>) {
        val hasViewAll = result.contains(systemKey(SfSystemPermission.VIEW_ALL_DATA))
        val hasModifyAll = result.contains(systemKey(SfSystemPermission.MODIFY_ALL_DATA))
        if (!hasViewAll && !hasModifyAll) return

        for ((entityTableName, _) in entitySfNameRegistry.snapshot()) {
            if (hasViewAll || hasModifyAll) {
                result.add(entityKey(entityTableName, SfPermissionOperation.READ))
            }
            if (hasModifyAll) {
                result.add(entityKey(entityTableName, SfPermissionOperation.CREATE))
                result.add(entityKey(entityTableName, SfPermissionOperation.EDIT))
                result.add(entityKey(entityTableName, SfPermissionOperation.DELETE))
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
