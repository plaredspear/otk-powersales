package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.AssignedUserSummary
import com.otoki.powersales.admin.permission.dto.EntityProfilePermission
import com.otoki.powersales.admin.permission.dto.EntityProfileRow
import com.otoki.powersales.admin.permission.dto.ObjectPermissionRow
import com.otoki.powersales.admin.permission.dto.PaginatedUserList
import com.otoki.powersales.admin.permission.dto.PermissionMatrix
import com.otoki.powersales.admin.permission.dto.PermissionMatrixProfile
import com.otoki.powersales.admin.permission.dto.PermissionSetDetail
import com.otoki.powersales.admin.permission.dto.PermissionSetFlagsSummary
import com.otoki.powersales.admin.permission.dto.PermissionSetSummary
import com.otoki.powersales.admin.permission.dto.ProfileDetail
import com.otoki.powersales.admin.permission.dto.ProfileFlagsSummary
import com.otoki.powersales.admin.permission.dto.ProfileSummary
import com.otoki.powersales.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.auth.permission.SfPermissionResolver
import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.common.config.CacheConfig
import org.springframework.cache.annotation.Cacheable
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetRepository
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Spec #803 — 권한 관리 admin 페이지 조회 service.
 *
 * Profile / PermissionSet / Assignment / entity×Profile Matrix 일람·상세 산출.
 * 모두 read-only. 부여/회수 편집은 spec #804 에서 별도 service.
 */
@Service
class AdminPermissionInspectionService(
    private val profileRepository: ProfileRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val permissionSetRepository: PermissionSetRepository,
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository,
    private val permissionSetAssignmentRepository: PermissionSetAssignmentRepository,
    private val userRepository: UserRepository,
    private val entitySfNameRegistry: EntitySfNameRegistry,
    private val sfPermissionResolver: SfPermissionResolver,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun listProfiles(): List<ProfileSummary> {
        return profileRepository.findAll().map { profile ->
            val flags = profileFlagsRepository.findByProfileId(profile.id)
            ProfileSummary(
                profileId = profile.id,
                name = profile.name,
                userType = profile.userType,
                description = profile.description,
                viewAllData = flags?.permissionsViewAllData ?: false,
                modifyAllData = flags?.permissionsModifyAllData ?: false,
                viewAllUsers = flags?.permissionsViewAllUsers ?: false,
                manageUsers = flags?.permissionsManageUsers ?: false,
                apiEnabled = flags?.permissionsApiEnabled ?: false,
                assignedUserCount = userRepository.countByProfileId(profile.id),
            )
        }.sortedBy { it.name }
    }

    @Transactional(readOnly = true)
    fun getProfile(profileId: Long, userPage: Int, userSize: Int, userKeyword: String?): ProfileDetail? {
        val profile = profileRepository.findById(profileId).orElse(null) ?: return null
        val flags = profileFlagsRepository.findByProfileId(profileId)

        val usersPage = userRepository.findUsersByProfileId(
            profileId = profileId,
            keyword = userKeyword,
            pageable = PageRequest.of(userPage, userSize),
        )

        return ProfileDetail(
            profileId = profile.id,
            name = profile.name,
            userType = profile.userType,
            description = profile.description,
            sfid = profile.sfid,
            flags = ProfileFlagsSummary(
                viewAllData = flags?.permissionsViewAllData ?: false,
                modifyAllData = flags?.permissionsModifyAllData ?: false,
                viewAllUsers = flags?.permissionsViewAllUsers ?: false,
                manageUsers = flags?.permissionsManageUsers ?: false,
                apiEnabled = flags?.permissionsApiEnabled ?: false,
            ),
            assignedUsers = usersPage.toPaginatedUserList(),
        )
    }

    @Transactional(readOnly = true)
    fun listPermissionSets(): List<PermissionSetSummary> {
        return permissionSetRepository.findAll().map { ps ->
            val flags = ps.sfid?.let { permissionSetFlagsRepository.findByPermissionSetSfid(it) }
            val objectPermissionCount = parseObjectPermissions(flags?.objectPermissions).size
            val flagsId = flags?.id
            val assignedUserCount = flagsId?.let {
                permissionSetAssignmentRepository.countByPermissionSetFlagsIdAndIsActiveTrue(it)
            } ?: 0L

            PermissionSetSummary(
                permissionSetId = ps.id,
                name = ps.name,
                label = ps.label,
                description = ps.description,
                permissionSetFlagsId = flagsId,
                viewAllData = flags?.permissionsViewAllData ?: false,
                modifyAllData = flags?.permissionsModifyAllData ?: false,
                objectPermissionCount = objectPermissionCount,
                assignedUserCount = assignedUserCount,
            )
        }.sortedBy { it.name }
    }

    @Transactional(readOnly = true)
    fun getPermissionSet(
        permissionSetId: Long,
        userPage: Int,
        userSize: Int,
        userKeyword: String?,
    ): PermissionSetDetail? {
        val ps = permissionSetRepository.findById(permissionSetId).orElse(null) ?: return null
        val flags = ps.sfid?.let { permissionSetFlagsRepository.findByPermissionSetSfid(it) }

        val objectPermissions = parseObjectPermissions(flags?.objectPermissions)
            .map { (sfApiName, perms) ->
                ObjectPermissionRow(
                    sfApiName = sfApiName,
                    entity = entitySfNameRegistry.toEntityTableName(sfApiName),
                    canRead = perms["allowRead"] == true,
                    canCreate = perms["allowCreate"] == true,
                    canEdit = perms["allowEdit"] == true,
                    canDelete = perms["allowDelete"] == true,
                )
            }
            .sortedBy { it.entity ?: it.sfApiName }

        val usersPage = flags?.let {
            userRepository.findUsersByPermissionSetFlagsId(
                permissionSetFlagsId = it.id,
                keyword = userKeyword,
                pageable = PageRequest.of(userPage, userSize),
            )
        }

        val paginatedUsers = usersPage?.toPaginatedUserList() ?: emptyPaginatedUserList(userPage, userSize)

        return PermissionSetDetail(
            permissionSetId = ps.id,
            name = ps.name,
            label = ps.label,
            description = ps.description,
            sfid = ps.sfid,
            flags = flags?.let {
                PermissionSetFlagsSummary(
                    permissionSetFlagsId = it.id,
                    viewAllData = it.permissionsViewAllData,
                    modifyAllData = it.permissionsModifyAllData,
                )
            },
            objectPermissions = objectPermissions,
            assignedUsers = paginatedUsers,
        )
    }

    /**
     * entity × Profile 매트릭스 산출 (Q2 옵션 1).
     *
     * Profile 별로 표본 user 1명 골라 SfPermissionResolver 호출 → 평탄화 결과를 entity 별로 분해.
     * 5분 TTL 캐시 — Profile/Assignment 변경 후 최대 5분 stale.
     */
    @Cacheable(CacheConfig.CACHE_PERMISSION_MATRIX, key = "'ALL'")
    @Transactional(readOnly = true)
    fun getMatrix(): PermissionMatrix {
        val profiles = profileRepository.findAll().sortedBy { it.name }
        val matrixProfiles = profiles.map { PermissionMatrixProfile(it.id, it.name) }

        val profileToPermissions: Map<Long, Set<String>> = profiles.associate { profile ->
            val sampleUser: User? = userRepository.findFirstByProfileId(profile.id)
            val permissions: Set<String> = sampleUser?.let { sfPermissionResolver.resolveForUser(it) } ?: emptySet()
            profile.id to permissions
        }

        val entitySnapshot = entitySfNameRegistry.snapshot().keys.sorted()
        val rows = entitySnapshot.map { entityTableName ->
            val byProfile = profiles.map { profile ->
                val perms = profileToPermissions[profile.id].orEmpty()
                EntityProfilePermission(
                    profileId = profile.id,
                    canRead = "$entityTableName:R" in perms,
                    canCreate = "$entityTableName:C" in perms,
                    canEdit = "$entityTableName:E" in perms,
                    canDelete = "$entityTableName:D" in perms,
                )
            }
            EntityProfileRow(entity = entityTableName, byProfile = byProfile)
        }

        return PermissionMatrix(profiles = matrixProfiles, rows = rows)
    }

    private fun parseObjectPermissions(json: String?): Map<String, Map<String, Boolean>> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            val raw = objectMapper.readValue(json, Map::class.java) as Map<String, Map<String, Boolean>>
            raw
        } catch (e: Exception) {
            log.warn("[AdminPermissionInspectionService] object_permissions JSON 파싱 실패: {}", e.message)
            emptyMap()
        }
    }

    private fun org.springframework.data.domain.Page<User>.toPaginatedUserList(): PaginatedUserList =
        PaginatedUserList(
            totalElements = totalElements,
            totalPages = totalPages,
            number = number,
            size = size,
            content = content.map { user ->
                AssignedUserSummary(
                    userId = user.id,
                    username = user.username,
                    employeeCode = user.employeeCode,
                    employeeName = user.name,
                )
            },
        )

    private fun emptyPaginatedUserList(page: Int, size: Int): PaginatedUserList =
        PaginatedUserList(totalElements = 0, totalPages = 0, number = page, size = size, content = emptyList())
}
