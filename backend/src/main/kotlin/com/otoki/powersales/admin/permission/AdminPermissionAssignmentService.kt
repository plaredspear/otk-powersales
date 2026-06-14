package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.AssignmentBatchItem
import com.otoki.powersales.admin.permission.dto.AssignmentBatchRequest
import com.otoki.powersales.admin.permission.dto.AssignmentBatchResult
import com.otoki.powersales.admin.permission.dto.AssignmentResponse
import com.otoki.powersales.admin.permission.exception.AssignmentAlreadyExistsException
import com.otoki.powersales.admin.permission.exception.AssignmentNotFoundException
import com.otoki.powersales.admin.permission.exception.AssignmentUserNotFoundException
import com.otoki.powersales.admin.permission.exception.CannotRevokeSelfException
import com.otoki.powersales.admin.permission.exception.LastAdminGuardException
import com.otoki.powersales.admin.permission.exception.PermissionSetFlagsNotFoundException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.platform.auth.permission.SfPermissionResolver
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetAssignment
import com.otoki.powersales.platform.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.platform.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.platform.common.config.CacheConfig
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Spec #804 — PermissionSetAssignment 부여/회수 service.
 *
 * 정책:
 * - 회수: soft delete (is_active=false) + updated_by_id 갱신 (Q1 옵션 1)
 * - 중복 부여: 409 (Q3 옵션 1) — 단 inactive row 가 있으면 재활성화 (Q4 옵션 1)
 * - 가드: MANAGE_USERS 시스템 권한 (Q2 옵션 1, controller 어노테이션)
 * - self-revoke + last-admin 차단 (§5.2)
 *
 * 부여/회수 후 `CACHE_PERMISSION_MATRIX` 일괄 evict.
 */
@Service
class AdminPermissionAssignmentService(
    private val assignmentRepository: PermissionSetAssignmentRepository,
    private val userRepository: UserRepository,
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository,
    private val sfPermissionResolver: SfPermissionResolver,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun assign(userId: Long, permissionSetFlagsId: Long, principalUserId: Long): AssignmentResponse {
        userRepository.findById(userId).orElseThrow { AssignmentUserNotFoundException(userId) }
        val flags = permissionSetFlagsRepository.findById(permissionSetFlagsId)
            .orElseThrow { PermissionSetFlagsNotFoundException(permissionSetFlagsId) }

        val existing = assignmentRepository.findByAssigneeUserIdAndPermissionSetFlagsId(userId, permissionSetFlagsId)
        if (existing != null && existing.isActive) {
            throw AssignmentAlreadyExistsException(userId, permissionSetFlagsId)
        }

        val saved = if (existing != null) {
            existing.isActive = true
            existing.updatedById = principalUserId
            assignmentRepository.save(existing)
        } else {
            // sfid 는 SF 데이터 마이그레이션 보조 필드 (Stage1 적재분만 박힘) — runtime 부여분은 null.
            // 신규 시스템에서는 id FK (assigneeUserId / permissionSetFlagsId) 만 운영 invariant.
            assignmentRepository.save(
                PermissionSetAssignment(
                    sfid = null,
                    assigneeUserSfid = null,
                    assigneeUserId = userId,
                    permissionSetSfid = null,
                    permissionSetFlagsId = permissionSetFlagsId,
                    isActive = true,
                    assignedAt = LocalDateTime.now(),
                    createdById = principalUserId,
                    updatedById = principalUserId,
                )
            )
        }
        adminPermissionCache.invalidate(userId)
        adminDataScopeCache.invalidate(userId)
        log.info(
            "[AdminPermissionAssignmentService] assigned userId={} permissionSetFlagsId={} by principalUserId={}",
            userId, permissionSetFlagsId, principalUserId,
        )
        return saved.toResponse()
    }

    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun revoke(assignmentId: Long, principalUserId: Long) {
        val assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow { AssignmentNotFoundException(assignmentId) }
        if (!assignment.isActive) {
            throw AssignmentNotFoundException(assignmentId)
        }

        // self-revoke 가드 — 본인의 MANAGE_USERS 부여 회수 시도 차단
        if (assignment.assigneeUserId == principalUserId && grantsManageUsers(assignment.permissionSetFlagsId)) {
            throw CannotRevokeSelfException()
        }

        assignment.isActive = false
        assignment.updatedById = principalUserId
        assignmentRepository.save(assignment)

        // last-admin 가드 — 회수 후 MANAGE_USERS 보유 active user 가 0 명이면 rollback
        if (grantsManageUsers(assignment.permissionSetFlagsId) && countActiveManageUsers() == 0L) {
            throw LastAdminGuardException()
        }

        assignment.assigneeUserId?.let {
            adminPermissionCache.invalidate(it)
            adminDataScopeCache.invalidate(it)
        }
        log.info(
            "[AdminPermissionAssignmentService] revoked assignmentId={} by principalUserId={}",
            assignmentId, principalUserId,
        )
    }

    @CacheEvict(value = [CacheConfig.CACHE_PERMISSION_MATRIX], allEntries = true)
    @Transactional
    fun assignBatch(request: AssignmentBatchRequest, principalUserId: Long): AssignmentBatchResult {
        val pairs = expandBatchPairs(request)

        val succeeded = mutableListOf<AssignmentBatchItem>()
        val skipped = mutableListOf<AssignmentBatchItem>()
        val failed = mutableListOf<AssignmentBatchItem>()

        for ((userId, flagsId) in pairs) {
            try {
                val resp = assign(userId, flagsId, principalUserId)
                succeeded += AssignmentBatchItem(userId, flagsId, assignmentId = resp.assignmentId)
            } catch (e: AssignmentAlreadyExistsException) {
                skipped += AssignmentBatchItem(userId, flagsId, reason = e.errorCode)
            } catch (e: Exception) {
                log.warn("[AdminPermissionAssignmentService] batch 부여 실패 userId={} flagsId={}: {}", userId, flagsId, e.message)
                failed += AssignmentBatchItem(userId, flagsId, reason = e.message ?: e.javaClass.simpleName)
            }
        }
        return AssignmentBatchResult(succeeded, skipped, failed)
    }

    private fun expandBatchPairs(request: AssignmentBatchRequest): List<Pair<Long, Long>> {
        val hasModeA = request.userId != null && !request.permissionSetFlagsIds.isNullOrEmpty()
        val hasModeB = request.permissionSetFlagsId != null && !request.userIds.isNullOrEmpty()

        require(hasModeA || hasModeB) { "Mode A (userId + permissionSetFlagsIds) 또는 Mode B (permissionSetFlagsId + userIds) 중 하나는 지정 필요" }
        require(!(hasModeA && hasModeB)) { "Mode A 와 Mode B 를 동시에 지정할 수 없습니다" }

        return when {
            hasModeA -> request.permissionSetFlagsIds!!.map { request.userId!! to it }
            else -> request.userIds!!.map { it to request.permissionSetFlagsId!! }
        }
    }

    private fun grantsManageUsers(permissionSetFlagsId: Long?): Boolean {
        if (permissionSetFlagsId == null) return false
        val flags = permissionSetFlagsRepository.findById(permissionSetFlagsId).orElse(null) ?: return false
        // PermissionSetFlags 에는 manage_users 비트 없음 — modify_all_data 가 effective MANAGE_USERS 포함
        return flags.permissionsModifyAllData
    }

    /**
     * MANAGE_USERS effective 권한 보유 active user 의 수.
     *
     * 단순화 — Profile 의 manage_users 비트 또는 MODIFY_ALL_DATA 보유.
     * 각 user 에 대해 SfPermissionResolver 호출 — 전체 user 순회는 비용 큼.
     * 본 가드는 회수 후 1회 호출이라 운영 size (~수백) 에서 허용 범위.
     */
    private fun countActiveManageUsers(): Long {
        return userRepository.findAll().count { user ->
            user.isActive && sfPermissionResolver.resolveForUser(user).contains(
                SfPermissionResolver.systemKey(SfSystemPermission.MANAGE_USERS)
            )
        }.toLong()
    }

    private fun PermissionSetAssignment.toResponse(): AssignmentResponse = AssignmentResponse(
        assignmentId = id,
        userId = assigneeUserId ?: 0L,
        permissionSetFlagsId = permissionSetFlagsId ?: 0L,
        isActive = isActive,
        assignedAt = assignedAt,
        createdById = createdById,
    )
}
