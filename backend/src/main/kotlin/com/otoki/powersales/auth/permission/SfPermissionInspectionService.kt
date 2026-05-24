package com.otoki.powersales.auth.permission

import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Spec #802 — 직원 상세 페이지의 권한 read-only 표시 용도 (Q4 옵션 1).
 *
 * Employee 의 employee_code → User → 부여된 PermissionSet + system permission 펼침.
 * SF org 가 부여 SoT 이므로 본 서비스는 edit API 를 제공하지 않는다.
 */
@Service
class SfPermissionInspectionService(
    private val userRepository: UserRepository,
    private val employeeRepository: EmployeeRepository,
    private val profileRepository: ProfileRepository,
    private val permissionSetAssignmentRepository: PermissionSetAssignmentRepository,
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val sfPermissionResolver: SfPermissionResolver,
) {

    @Transactional(readOnly = true)
    fun inspectByEmployeeId(employeeId: Long): EmployeePermissionInspection? {
        val employee = employeeRepository.findById(employeeId).orElse(null) ?: return null
        return inspectByEmployeeCode(employee.employeeCode)
    }

    @Transactional(readOnly = true)
    fun inspectByEmployeeCode(employeeCode: String): EmployeePermissionInspection? {
        val user = userRepository.findByEmployeeCode(employeeCode) ?: return null

        val permissionSets = permissionSetAssignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(user.id)
            .mapNotNull { assignment ->
                val flagsId = assignment.permissionSetFlagsId ?: return@mapNotNull null
                permissionSetFlagsRepository.findById(flagsId).orElse(null)?.let { flags ->
                    AssignedPermissionSet(
                        assignmentId = assignment.id,
                        permissionSetFlagsId = flagsId,
                        permissionSetName = flags.permissionSetName,
                        permissionSetSfid = flags.permissionSetSfid,
                        viewAllData = flags.permissionsViewAllData,
                        modifyAllData = flags.permissionsModifyAllData,
                    )
                }
            }

        val profileFlags = user.profileId?.let { profileFlagsRepository.findByProfileId(it) }
        val profileSummary = profileFlags?.let { flags ->
            val profileName = profileRepository.findById(flags.profileId).orElse(null)?.name ?: "(unknown)"
            ProfileSummary(
                profileName = profileName,
                viewAllData = flags.permissionsViewAllData,
                modifyAllData = flags.permissionsModifyAllData,
                viewAllUsers = flags.permissionsViewAllUsers,
                manageUsers = flags.permissionsManageUsers,
                apiEnabled = flags.permissionsApiEnabled,
            )
        }

        val resolvedKeys = sfPermissionResolver.resolveForUser(user)
        val entityMatrix = resolvedKeys
            .filter { !it.startsWith("SYSTEM:") }
            .groupBy { it.substringBefore(":") }
            .map { (entity, keys) ->
                val ops = keys.map { it.substringAfter(":") }.toSet()
                EntityPermissionRow(
                    entity = entity,
                    canRead = "R" in ops,
                    canCreate = "C" in ops,
                    canEdit = "E" in ops,
                    canDelete = "D" in ops,
                )
            }
            .sortedBy { it.entity }

        val systemPermissions = resolvedKeys
            .filter { it.startsWith("SYSTEM:") }
            .map { it.removePrefix("SYSTEM:") }
            .sorted()

        return EmployeePermissionInspection(
            employeeCode = employeeCode,
            userId = user.id,
            username = user.username,
            profile = profileSummary,
            permissionSets = permissionSets,
            entityMatrix = entityMatrix,
            systemPermissions = systemPermissions,
        )
    }

    data class EmployeePermissionInspection(
        val employeeCode: String,
        val userId: Long,
        val username: String,
        val profile: ProfileSummary?,
        val permissionSets: List<AssignedPermissionSet>,
        val entityMatrix: List<EntityPermissionRow>,
        val systemPermissions: List<String>,
    )

    data class ProfileSummary(
        val profileName: String,
        val viewAllData: Boolean,
        val modifyAllData: Boolean,
        val viewAllUsers: Boolean,
        val manageUsers: Boolean,
        val apiEnabled: Boolean,
    )

    data class AssignedPermissionSet(
        val assignmentId: Long,
        val permissionSetFlagsId: Long,
        val permissionSetName: String,
        // V197 — PSF 의 permission_set_sfid 가 Stage1 적재 시점 NULL 허용. Stage2 fk substep 직후
        // 정상 값으로 채워지나 운영 중간 상태 표현 위해 nullable.
        val permissionSetSfid: String?,
        val viewAllData: Boolean,
        val modifyAllData: Boolean,
    )

    data class EntityPermissionRow(
        val entity: String,
        val canRead: Boolean,
        val canCreate: Boolean,
        val canEdit: Boolean,
        val canDelete: Boolean,
    )
}
