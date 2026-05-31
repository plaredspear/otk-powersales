package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.AssignedPermissionSetUserSummary
import com.otoki.powersales.admin.permission.dto.AssignedUserSummary
import com.otoki.powersales.admin.permission.dto.CustomPermissionRow
import com.otoki.powersales.admin.permission.dto.EntityProfilePermission
import com.otoki.powersales.admin.permission.dto.PaginatedPermissionSetUserList
import com.otoki.powersales.admin.permission.dto.EntityProfileRow
import com.otoki.powersales.admin.permission.dto.ObjectPermissionRow
import com.otoki.powersales.admin.permission.dto.PaginatedUserList
import com.otoki.powersales.admin.permission.dto.PermissionMatrix
import com.otoki.powersales.admin.permission.dto.PermissionMatrixProfile
import com.otoki.powersales.admin.permission.dto.PermissionSetDetail
import com.otoki.powersales.admin.permission.dto.PermissionSetMatrix
import com.otoki.powersales.admin.permission.dto.PermissionSetMatrixEntry
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
 * 본 service 자체는 read-only — 부여/회수 편집은 spec #804 [AdminPermissionAssignmentService],
 * PS CRUD + 권한 비트 수정은 spec #837 [AdminPermissionSetMutationService] 가 분담.
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

        // PermissionSet 상세와 동일 — object/custom 권한 JSON 을 행 형식으로 파싱 (편집 화면 현재값 표시용).
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

        val customPermissions = parsePermissionsJson(flags?.customPermissions, "custom_permissions")
            .map { (resourceName, perms) ->
                CustomPermissionRow(
                    resource = resourceName,
                    canRead = perms["allowRead"] == true,
                    canCreate = perms["allowCreate"] == true,
                    canEdit = perms["allowEdit"] == true,
                    canDelete = perms["allowDelete"] == true,
                )
            }
            .sortedBy { it.resource }

        return ProfileDetail(
            profileId = profile.id,
            name = profile.name,
            userType = profile.userType,
            description = profile.description,
            // sfid 는 SF 데이터 마이그레이션 보조 필드 — API 응답에 노출 금지 (정책).
            flags = ProfileFlagsSummary(
                viewAllData = flags?.permissionsViewAllData ?: false,
                modifyAllData = flags?.permissionsModifyAllData ?: false,
                viewAllUsers = flags?.permissionsViewAllUsers ?: false,
                manageUsers = flags?.permissionsManageUsers ?: false,
                apiEnabled = flags?.permissionsApiEnabled ?: false,
            ),
            objectPermissions = objectPermissions,
            customPermissions = customPermissions,
            isLocallyModified = flags?.isLocallyModified ?: false,
            assignedUsers = usersPage.toPaginatedUserList(),
        )
    }

    @Transactional(readOnly = true)
    fun listPermissionSets(): List<PermissionSetSummary> {
        return permissionSetRepository.findAll().map { ps ->
            val flags = permissionSetFlagsRepository.findByPermissionSetId(ps.id)
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
                sfOrigin = ps.sfid != null,
                isLocallyModified = flags?.isLocallyModified ?: false,
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
        val flags = permissionSetFlagsRepository.findByPermissionSetId(ps.id)

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

        // spec #808 — custom resource (가상 자원) 권한 비트
        val customPermissions = parsePermissionsJson(flags?.customPermissions, "custom_permissions")
            .map { (resourceName, perms) ->
                CustomPermissionRow(
                    resource = resourceName,
                    canRead = perms["allowRead"] == true,
                    canCreate = perms["allowCreate"] == true,
                    canEdit = perms["allowEdit"] == true,
                    canDelete = perms["allowDelete"] == true,
                )
            }
            .sortedBy { it.resource }

        val paginatedUsers = flags?.let {
            val usersPage = userRepository.findUsersByPermissionSetFlagsId(
                permissionSetFlagsId = it.id,
                keyword = userKeyword,
                pageable = PageRequest.of(userPage, userSize),
            )

            // 한 user 가 동일 permissionSetFlags 에 active row 1개 (V187 partial unique) 라
            // assignmentId lookup 은 in-clause 단일 조회로 처리 — N+1 회피.
            val assignmentByUserId = permissionSetAssignmentRepository
                .findAllByPermissionSetFlagsIdAndIsActiveTrue(it.id)
                .associateBy { a -> a.assigneeUserId }

            PaginatedPermissionSetUserList(
                totalElements = usersPage.totalElements,
                totalPages = usersPage.totalPages,
                number = usersPage.number,
                size = usersPage.size,
                content = usersPage.content.map { user ->
                    AssignedPermissionSetUserSummary(
                        assignmentId = assignmentByUserId[user.id]?.id ?: 0L,
                        userId = user.id,
                        username = user.username,
                        employeeCode = user.employeeCode,
                        employeeName = user.name,
                    )
                },
            )
        } ?: PaginatedPermissionSetUserList(
            totalElements = 0, totalPages = 0, number = userPage, size = userSize, content = emptyList(),
        )

        return PermissionSetDetail(
            permissionSetId = ps.id,
            name = ps.name,
            label = ps.label,
            description = ps.description,
            // sfid 는 SF 데이터 마이그레이션 보조 필드 — API 응답에 노출 금지 (정책). Spec #837 결정 1-A:
            // SF 출처 여부는 sfOrigin boolean 으로만 노출.
            flags = flags?.let {
                PermissionSetFlagsSummary(
                    permissionSetFlagsId = it.id,
                    viewAllData = it.permissionsViewAllData,
                    modifyAllData = it.permissionsModifyAllData,
                )
            },
            objectPermissions = objectPermissions,
            customPermissions = customPermissions,
            assignedUsers = paginatedUsers,
            sfOrigin = ps.sfid != null,
            isLocallyModified = flags?.isLocallyModified ?: false,
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

        // 권한 자원 카탈로그 (allResources) 기준 — @SFObject 미부착 신규 entity + @PermissionResource 가상 자원까지
        // 포함. 권한 가드(@RequiresSfPermission) / 평탄화(SfPermissionResolver) 와 동일 카탈로그라야 매트릭스가
        // 실제 권한 부여/검사 대상과 일치. snapshot() (= SF 매핑 entity 만) 사용 시 SF 비대응 자원이 매트릭스에서 누락.
        val entitySnapshot = entitySfNameRegistry.allResources().sorted()
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

    /**
     * 모든 PermissionSet 의 시스템권한 flag + entity 객체권한 매트릭스 일괄 반환.
     *
     * "페이지별 필요 권한" 가이드 페이지가 사용. user 페이징이 없는 read-only 산출이라 캐시 없이
     * 매 호출 fresh.
     */
    @Transactional(readOnly = true)
    fun getPermissionSetMatrix(): PermissionSetMatrix {
        val entries = permissionSetRepository.findAll().map { ps ->
            val flags = permissionSetFlagsRepository.findByPermissionSetId(ps.id)
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

            PermissionSetMatrixEntry(
                permissionSetId = ps.id,
                name = ps.name,
                label = ps.label,
                viewAllData = flags?.permissionsViewAllData ?: false,
                modifyAllData = flags?.permissionsModifyAllData ?: false,
                objectPermissions = objectPermissions,
            )
        }.sortedBy { it.name }

        return PermissionSetMatrix(permissionSets = entries)
    }

    private fun parseObjectPermissions(json: String?): Map<String, Map<String, Boolean>> =
        parsePermissionsJson(json, "object_permissions")

    private fun parsePermissionsJson(json: String?, label: String): Map<String, Map<String, Boolean>> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            val raw = objectMapper.readValue(json, Map::class.java) as Map<String, Map<String, Boolean>>
            raw
        } catch (e: Exception) {
            log.warn("[AdminPermissionInspectionService] {} JSON 파싱 실패: {}", label, e.message)
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
}
