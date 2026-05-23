package com.otoki.powersales.auth.permission

import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 운영 cut-over 사전 검증 도구 (spec #801 §4.1).
 *
 * 입력: 검증 대상 user 일람 + 검증 대상 (entity, operation) pair 일람.
 * 출력: user 별 통과/차단 매트릭스 + 차단 사유 (어떤 SF permission 부여가 없어서 차단됐는지).
 *
 * cut-over 전 admin 의 PermissionSet 부여 적정성 확인 용도. cut-over 안정화 후 폐기 검토.
 */
@Service
class SfPermissionDryRunService(
    private val userRepository: UserRepository,
    private val sfPermissionResolver: SfPermissionResolver,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 다수 user 의 다수 (entity, operation) pair 에 대해 통과/차단 매트릭스 산출.
     *
     * @param userIds 검증 대상 user id 일람 (null 인 경우 employeeCodes 사용)
     * @param employeeCodes 검증 대상 employee code 일람 (userIds 와 동시 지정 시 합집합)
     * @param checks 검증 대상 (entity, operation, systemPermission) 일람. operation=SYSTEM 인 경우만 systemPermission 사용.
     */
    @Transactional(readOnly = true)
    fun dryRun(
        userIds: Set<Long> = emptySet(),
        employeeCodes: Set<String> = emptySet(),
        checks: List<DryRunCheck>,
    ): DryRunResult {
        val users = mutableListOf<UserSnapshot>()
        if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).forEach { user ->
                users += UserSnapshot(user.id, user.username, user.employeeCode, sfPermissionResolver.resolveForUser(user))
            }
        }
        if (employeeCodes.isNotEmpty()) {
            employeeCodes.forEach { code ->
                userRepository.findByEmployeeCode(code)?.let { user ->
                    if (users.none { it.userId == user.id }) {
                        users += UserSnapshot(user.id, user.username, user.employeeCode, sfPermissionResolver.resolveForUser(user))
                    }
                }
            }
        }

        val rows = users.map { snapshot ->
            val checkResults = checks.map { check ->
                val allowed = isAllowed(check, snapshot.permissions)
                val reason = if (allowed) null else buildBlockReason(check)
                CheckResult(check = check, allowed = allowed, reason = reason)
            }
            UserDryRunRow(
                userId = snapshot.userId,
                username = snapshot.username,
                employeeCode = snapshot.employeeCode,
                permissionsCount = snapshot.permissions.size,
                checks = checkResults,
            )
        }

        val totalChecks = rows.size * checks.size
        val allowedCount = rows.sumOf { row -> row.checks.count { it.allowed } }
        log.info("[SfPermissionDryRunService] users={}, checks={}, allowed={}/{}", users.size, checks.size, allowedCount, totalChecks)

        return DryRunResult(
            userCount = users.size,
            checkCount = checks.size,
            allowedCount = allowedCount,
            blockedCount = totalChecks - allowedCount,
            rows = rows,
        )
    }

    private fun isAllowed(check: DryRunCheck, permissions: Set<String>): Boolean {
        return when (check.operation) {
            SfPermissionOperation.SYSTEM -> {
                val sysPerm = check.systemPermission ?: return false
                permissions.contains(SfPermissionResolver.systemKey(sysPerm))
            }
            else -> {
                val entity = check.entity ?: return false
                permissions.contains(SfPermissionResolver.entityKey(entity, check.operation))
            }
        }
    }

    private fun buildBlockReason(check: DryRunCheck): String {
        return when (check.operation) {
            SfPermissionOperation.SYSTEM -> {
                "ProfileFlags / PermissionSetFlags 의 ${check.systemPermission?.columnName} 비트가 TRUE 인 부여 없음"
            }
            else -> {
                "PermissionSetFlags.object_permissions 의 ${check.entity} (entity) ${check.operation} 비트가 TRUE 인 부여 없음"
            }
        }
    }

    data class DryRunCheck(
        val entity: String? = null,
        val operation: SfPermissionOperation,
        val systemPermission: SfSystemPermission? = null,
    )

    data class DryRunResult(
        val userCount: Int,
        val checkCount: Int,
        val allowedCount: Int,
        val blockedCount: Int,
        val rows: List<UserDryRunRow>,
    )

    data class UserDryRunRow(
        val userId: Long,
        val username: String,
        val employeeCode: String?,
        val permissionsCount: Int,
        val checks: List<CheckResult>,
    )

    data class CheckResult(
        val check: DryRunCheck,
        val allowed: Boolean,
        val reason: String?,
    )

    private data class UserSnapshot(
        val userId: Long,
        val username: String,
        val employeeCode: String?,
        val permissions: Set<String>,
    )
}
