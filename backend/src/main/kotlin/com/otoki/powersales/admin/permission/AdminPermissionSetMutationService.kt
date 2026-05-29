package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.PermissionSetChangeLogResponse
import com.otoki.powersales.admin.permission.dto.PermissionSetCreateRequest
import com.otoki.powersales.admin.permission.dto.PermissionSetMutationResponse
import com.otoki.powersales.admin.permission.dto.PermissionSetUpdateFlagsRequest
import com.otoki.powersales.admin.permission.dto.PermissionSetUpdateMetaRequest
import com.otoki.powersales.admin.permission.exception.InvalidCustomPermissionKeyException
import com.otoki.powersales.admin.permission.exception.InvalidObjectPermissionKeyException
import com.otoki.powersales.admin.permission.exception.PermissionSetNameAlreadyExistsException
import com.otoki.powersales.admin.permission.exception.PermissionSetNameInvalidException
import com.otoki.powersales.admin.permission.exception.PermissionSetNotFoundException
import com.otoki.powersales.admin.permission.exception.SfOriginDeleteBlockedException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.auth.permission.AdminPermissionCache
import com.otoki.powersales.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.auth.sharing.entity.PermissionSet
import com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLog
import com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLogEventType
import com.otoki.powersales.auth.sharing.entity.PermissionSetFlags
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetChangeLogRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetRepository
import com.otoki.powersales.common.config.CacheConfig
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Spec #837 — PermissionSet 자체 관리 (CRUD + 권한 비트 수정 + 변경 이력 적재) service.
 *
 * 정책:
 * - 가드: MANAGE_USERS 시스템 권한 (controller @RequiresSfPermission, Q6 옵션 1)
 * - 변경 이력: `permission_set_change_log` 테이블에 4 event type 적재 (Q2 옵션 1)
 * - dirty 플래그 (`is_locally_modified`): SF 출처 PS (sfid IS NOT NULL) 가 신규 시스템에서 수정되면 set
 *   (Q3 옵션 1). Stage1 재적재 service 변경은 후속 (#837 결정 2-D 옵션, 운영 절차로 보호).
 * - 삭제: SF 출처 PS 는 409 차단 / 신규 PS 는 assignment cascade hard delete (Q4 옵션 1)
 * - 권한 비트 키 검증: EntitySfNameRegistry 의 snapshot()/allResources() 기준 — 미등록 키 400
 * - 캐시: 모든 mutation 후 CACHE_PERMISSION_MATRIX allEntries evict + AdminPermissionCache invalidateAll
 *
 * 패키지 배치: 기존 #804 AdminPermissionAssignmentService 와 동일 admin/permission/ 잔류 (회색지대).
 * 향후 권한 도메인 일괄 재배치 시 (기존 #804 service 와 함께) auth/sharing/service/ 로 이동 TODO.
 */
@Service
class AdminPermissionSetMutationService(
    private val permissionSetRepository: PermissionSetRepository,
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository,
    private val permissionSetAssignmentRepository: PermissionSetAssignmentRepository,
    private val permissionSetChangeLogRepository: PermissionSetChangeLogRepository,
    private val entitySfNameRegistry: EntitySfNameRegistry,
    private val userRepository: UserRepository,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 신규 PermissionSet 생성.
     *
     * permission_set + 빈 permission_set_flags 동시 INSERT + change_log CREATE 1건. sfid = NULL (신규 자체 PS).
     * name 형식 검증 (영문/숫자/언더스코어 + 80자 이내) / unique 검증.
     */
    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun create(request: PermissionSetCreateRequest, principalUserId: Long): PermissionSetMutationResponse {
        validateName(request.name)
        if (permissionSetRepository.findByName(request.name) != null) {
            throw PermissionSetNameAlreadyExistsException(request.name)
        }

        val ps = permissionSetRepository.save(
            PermissionSet(
                sfid = null,
                name = request.name,
                label = request.label,
                description = request.description,
            ),
        )
        val flags = permissionSetFlagsRepository.save(
            PermissionSetFlags(
                permissionSetSfid = null,
                permissionSetName = request.name,
                permissionsViewAllData = false,
                permissionsModifyAllData = false,
                objectPermissions = "{}",
                customPermissions = "{}",
                permissionSetId = ps.id,
                isLocallyModified = false,
            ),
        )
        val response = buildResponse(ps, flags)
        recordChangeLog(
            permissionSetId = ps.id,
            eventType = PermissionSetChangeLogEventType.CREATE,
            beforeSnapshot = null,
            afterSnapshot = response,
            principalUserId = principalUserId,
        )

        invalidateCaches()
        log.info(
            "[AdminPermissionSetMutationService] created permissionSetId={} name={} principalUserId={}",
            ps.id, ps.name, principalUserId,
        )
        return response
    }

    /**
     * PermissionSet 메타 (label / description) 수정.
     *
     * name 은 식별자 안정성을 위해 수정 불가. SF 출처 PS (sfid 보유) 이면 flags 의 is_locally_modified set.
     */
    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun updateMeta(
        permissionSetId: Long,
        request: PermissionSetUpdateMetaRequest,
        principalUserId: Long,
    ): PermissionSetMutationResponse {
        val ps = permissionSetRepository.findById(permissionSetId).orElseThrow {
            PermissionSetNotFoundException(permissionSetId)
        }
        val flags = permissionSetFlagsRepository.findByPermissionSetId(permissionSetId)

        val before = buildResponse(ps, flags)

        ps.label = request.label
        ps.description = request.description
        permissionSetRepository.save(ps)

        if (ps.sfid != null && flags != null && !flags.isLocallyModified) {
            flags.isLocallyModified = true
            permissionSetFlagsRepository.save(flags)
        }

        val after = buildResponse(ps, permissionSetFlagsRepository.findByPermissionSetId(permissionSetId))
        recordChangeLog(
            permissionSetId = ps.id,
            eventType = PermissionSetChangeLogEventType.UPDATE_META,
            beforeSnapshot = before,
            afterSnapshot = after,
            principalUserId = principalUserId,
        )

        invalidateCaches()
        log.info(
            "[AdminPermissionSetMutationService] updated meta permissionSetId={} principalUserId={}",
            ps.id, principalUserId,
        )
        return after
    }

    /**
     * PermissionSet 권한 비트 (system / object / custom) 전체 교체.
     *
     * objectPermissions 키는 EntitySfNameRegistry SF 매핑 entity 만 허용. customPermissions 키는
     * SF 매핑 외 자원 (`@PermissionResource`) 만 허용. SF 출처 PS 면 dirty 플래그 set.
     */
    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun updateFlags(
        permissionSetId: Long,
        request: PermissionSetUpdateFlagsRequest,
        principalUserId: Long,
    ): PermissionSetMutationResponse {
        val ps = permissionSetRepository.findById(permissionSetId).orElseThrow {
            PermissionSetNotFoundException(permissionSetId)
        }
        val flags = permissionSetFlagsRepository.findByPermissionSetId(permissionSetId)
            ?: error("PermissionSetFlags 누락 — permissionSetId=$permissionSetId. 기존 PS 적재 정합 위반 (Stage1 확인 필요)")

        validateObjectPermissionKeys(request.objectPermissions)
        validateCustomPermissionKeys(request.customPermissions)

        val before = buildResponse(ps, flags)

        flags.permissionsViewAllData = request.viewAllData
        flags.permissionsModifyAllData = request.modifyAllData
        flags.objectPermissions = objectMapper.writeValueAsString(request.objectPermissions)
        flags.customPermissions = objectMapper.writeValueAsString(request.customPermissions)
        if (ps.sfid != null) {
            flags.isLocallyModified = true
        }
        permissionSetFlagsRepository.save(flags)

        val after = buildResponse(ps, flags)
        recordChangeLog(
            permissionSetId = ps.id,
            eventType = PermissionSetChangeLogEventType.UPDATE_FLAGS,
            beforeSnapshot = before,
            afterSnapshot = after,
            principalUserId = principalUserId,
        )

        invalidateCaches()
        log.info(
            "[AdminPermissionSetMutationService] updated flags permissionSetId={} dirty={} principalUserId={}",
            ps.id, flags.isLocallyModified, principalUserId,
        )
        return after
    }

    /**
     * PermissionSet 삭제.
     *
     * SF 출처 (sfid IS NOT NULL) 면 409 SF_ORIGIN_DELETE_BLOCKED. 신규 자체 PS 면 부여 assignment 일괄
     * hard delete + flags hard delete + ps hard delete. change_log 의 permission_set_id 는
     * ON DELETE SET NULL 정책으로 NULL 처리되어 audit 보존.
     */
    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun delete(permissionSetId: Long, principalUserId: Long) {
        val ps = permissionSetRepository.findById(permissionSetId).orElseThrow {
            PermissionSetNotFoundException(permissionSetId)
        }
        if (ps.sfid != null) {
            throw SfOriginDeleteBlockedException(permissionSetId)
        }

        val flags = permissionSetFlagsRepository.findByPermissionSetId(permissionSetId)
        val beforeSnapshot = buildDeleteSnapshot(ps, flags)

        if (flags != null) {
            val assignments = permissionSetAssignmentRepository.findAllByPermissionSetFlagsId(flags.id)
            if (assignments.isNotEmpty()) {
                permissionSetAssignmentRepository.deleteAll(assignments)
                assignments.forEach { a -> a.assigneeUserId?.let { adminPermissionCache.invalidate(it) } }
            }
            permissionSetFlagsRepository.delete(flags)
        }
        permissionSetRepository.delete(ps)

        recordChangeLog(
            permissionSetId = null, // PS 자체가 삭제되어 FK SET NULL 동기
            eventType = PermissionSetChangeLogEventType.DELETE,
            beforeSnapshot = beforeSnapshot,
            afterSnapshot = null,
            principalUserId = principalUserId,
        )

        invalidateCaches()
        log.info(
            "[AdminPermissionSetMutationService] deleted permissionSetId={} name={} principalUserId={}",
            permissionSetId, ps.name, principalUserId,
        )
    }

    /**
     * 특정 PS 의 변경 이력 페이지네이션 조회.
     *
     * changedAt desc 정렬. PS 삭제 후 본 메서드로는 조회 불가 (FK SET NULL).
     */
    @Transactional(readOnly = true)
    fun listChangeLog(permissionSetId: Long, page: Int, size: Int): Page<PermissionSetChangeLogResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "changedAt"))
        val rows = permissionSetChangeLogRepository.findByPermissionSetId(permissionSetId, pageable)
        val userNames: Map<Long, String?> = rows.content.map { it.changedById }.distinct().associateWith { userId ->
            userRepository.findById(userId).orElse(null)?.name
        }
        return rows.map { row ->
            PermissionSetChangeLogResponse(
                changeLogId = row.id,
                permissionSetId = row.permissionSetId,
                eventType = row.eventType.name,
                beforeSnapshot = row.beforeSnapshot,
                afterSnapshot = row.afterSnapshot,
                changedById = row.changedById,
                changedByName = userNames[row.changedById],
                changedAt = row.changedAt,
                changeReason = row.changeReason,
            )
        }
    }

    private fun buildResponse(ps: PermissionSet, flags: PermissionSetFlags?): PermissionSetMutationResponse {
        return PermissionSetMutationResponse(
            permissionSetId = ps.id,
            name = ps.name,
            label = ps.label,
            description = ps.description,
            sfOrigin = ps.sfid != null,
            permissionSetFlagsId = flags?.id,
            viewAllData = flags?.permissionsViewAllData ?: false,
            modifyAllData = flags?.permissionsModifyAllData ?: false,
            objectPermissions = parseJsonOrEmpty(flags?.objectPermissions),
            customPermissions = parseJsonOrEmpty(flags?.customPermissions),
            isLocallyModified = flags?.isLocallyModified ?: false,
        )
    }

    private fun buildDeleteSnapshot(ps: PermissionSet, flags: PermissionSetFlags?): PermissionSetDeleteSnapshot {
        val assignmentSnapshots = flags?.let {
            permissionSetAssignmentRepository.findAllByPermissionSetFlagsId(it.id).map { a ->
                PermissionSetDeleteSnapshot.AssignmentEntry(
                    assignmentId = a.id,
                    assigneeUserId = a.assigneeUserId,
                    isActive = a.isActive,
                )
            }
        } ?: emptyList()
        return PermissionSetDeleteSnapshot(
            permissionSetId = ps.id,
            name = ps.name,
            label = ps.label,
            description = ps.description,
            viewAllData = flags?.permissionsViewAllData ?: false,
            modifyAllData = flags?.permissionsModifyAllData ?: false,
            objectPermissions = parseJsonOrEmpty(flags?.objectPermissions),
            customPermissions = parseJsonOrEmpty(flags?.customPermissions),
            assignments = assignmentSnapshots,
        )
    }

    private fun recordChangeLog(
        permissionSetId: Long?,
        eventType: PermissionSetChangeLogEventType,
        beforeSnapshot: Any?,
        afterSnapshot: Any?,
        principalUserId: Long,
    ) {
        permissionSetChangeLogRepository.save(
            PermissionSetChangeLog(
                permissionSetId = permissionSetId,
                eventType = eventType,
                beforeSnapshot = beforeSnapshot?.let { objectMapper.writeValueAsString(it) },
                afterSnapshot = afterSnapshot?.let { objectMapper.writeValueAsString(it) },
                changedById = principalUserId,
            ),
        )
    }

    private fun invalidateCaches() {
        adminPermissionCache.invalidateAll()
        adminDataScopeCache.invalidateAll()
    }

    private fun validateName(name: String) {
        if (name.isBlank()) throw PermissionSetNameInvalidException(name, "빈 값")
        if (name.length > 80) throw PermissionSetNameInvalidException(name, "80자 초과")
        if (!NAME_PATTERN.matches(name)) {
            throw PermissionSetNameInvalidException(name, "영문/숫자/언더스코어 외 문자 포함")
        }
    }

    private fun validateObjectPermissionKeys(map: Map<String, Map<String, Boolean>>) {
        val sfNames = entitySfNameRegistry.snapshot().values.toSet()
        for (key in map.keys) {
            if (key !in sfNames) throw InvalidObjectPermissionKeyException(key)
        }
    }

    private fun validateCustomPermissionKeys(map: Map<String, Map<String, Boolean>>) {
        val sfMappedEntities = entitySfNameRegistry.snapshot().keys
        val allResources = entitySfNameRegistry.allResources()
        // custom 자원 = allResources - SF 매핑 entity (objectPermissions 영역과 분리)
        val customResources = allResources - sfMappedEntities
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
            log.warn("[AdminPermissionSetMutationService] permissions JSON 파싱 실패: {}", e.message)
            emptyMap()
        }
    }

    companion object {
        private val NAME_PATTERN = Regex("^[A-Za-z0-9_]+$")
    }

    /** DELETE snapshot 의 nested DTO — change_log.before_snapshot JSON 으로 직렬화. */
    private data class PermissionSetDeleteSnapshot(
        val permissionSetId: Long,
        val name: String,
        val label: String?,
        val description: String?,
        val viewAllData: Boolean,
        val modifyAllData: Boolean,
        val objectPermissions: Map<String, Map<String, Boolean>>,
        val customPermissions: Map<String, Map<String, Boolean>>,
        val assignments: List<AssignmentEntry>,
    ) {
        data class AssignmentEntry(
            val assignmentId: Long,
            val assigneeUserId: Long?,
            val isActive: Boolean,
        )
    }
}
