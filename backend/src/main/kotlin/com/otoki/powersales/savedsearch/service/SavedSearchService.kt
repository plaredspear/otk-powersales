package com.otoki.powersales.savedsearch.service

import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfPermissionResolver
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.savedsearch.SavedSearchPresets
import com.otoki.powersales.savedsearch.dto.request.SavedSearchCreateRequest
import com.otoki.powersales.savedsearch.dto.request.SavedSearchUpdateRequest
import com.otoki.powersales.savedsearch.dto.response.SavedSearchResponse
import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import com.otoki.powersales.savedsearch.exception.SavedSearchDuplicateNameException
import com.otoki.powersales.savedsearch.exception.SavedSearchForbiddenException
import com.otoki.powersales.savedsearch.exception.SavedSearchNotFoundException
import com.otoki.powersales.savedsearch.repository.SavedSearchRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 저장된 검색 (SavedSearch) 서비스 (Spec #852).
 *
 * 스코프(PRIVATE/SHARED)별 가시성·권한 분기를 처리한다. SHARED 의 생성/수정/삭제는 `saved_search`
 * EDIT 권한 보유자만 가능하며, 본 검증은 호출자의 권한 snapshot(`permissions`)으로 한다 — PRIVATE/SHARED
 * 가 한 엔드포인트에 혼재하므로 컨트롤러 어노테이션 가드가 아닌 서비스 내부 분기로 처리한다.
 */
@Service
@Transactional(readOnly = true)
class SavedSearchService(
    private val savedSearchRepository: SavedSearchRepository,
    private val employeeRepository: EmployeeRepository,
) {

    private val sharedEditKey: String =
        SfPermissionResolver.entityKey("saved_search", SfPermissionOperation.EDIT)

    /**
     * 목록 조회 — (시스템 기본 공용 프리셋) ∪ (본인 PRIVATE) ∪ (전체 SHARED).
     *
     * 시스템 프리셋([SavedSearchPresets])은 DB 가 아닌 코드 SoT 라서 응답에 합성해 맨 앞에 끼워넣는다.
     * 항상 editable=false (운영자도 수정·삭제 불가).
     */
    fun list(resourceKey: String, employeeId: Long, permissions: Set<String>): List<SavedSearchResponse> {
        val systemPresets = SavedSearchPresets.systemPresetsFor(resourceKey).map { preset ->
            SavedSearchResponse.of(entity = preset, ownerName = null, editable = false)
        }
        val items = savedSearchRepository.findVisible(resourceKey, employeeId)
        val ownerNames = resolveOwnerNames(items.map { it.ownerId })
        val canEditShared = permissions.contains(sharedEditKey)
        val stored = items.map { entity ->
            SavedSearchResponse.of(
                entity = entity,
                ownerName = ownerNames[entity.ownerId],
                editable = isEditable(entity, employeeId, canEditShared),
            )
        }
        return systemPresets + stored
    }

    @Transactional
    fun create(
        request: SavedSearchCreateRequest,
        employeeId: Long,
        permissions: Set<String>,
    ): SavedSearchResponse {
        if (request.scope == SavedSearchScope.SHARED) {
            requireSharedEditPermission(permissions)
        }
        if (savedSearchRepository.existsByResourceKeyAndOwnerIdAndScopeAndName(
                request.resourceKey, employeeId, request.scope, request.name,
            )
        ) {
            throw SavedSearchDuplicateNameException()
        }
        val saved = savedSearchRepository.save(
            SavedSearch(
                resourceKey = request.resourceKey,
                name = request.name,
                scope = request.scope,
                ownerId = employeeId,
                filters = request.filters,
                sortOrder = request.sortOrder,
            ),
        )
        return toResponse(saved, employeeId, permissions.contains(sharedEditKey))
    }

    @Transactional
    fun update(
        id: Long,
        request: SavedSearchUpdateRequest,
        employeeId: Long,
        permissions: Set<String>,
    ): SavedSearchResponse {
        if (SavedSearchPresets.isSystemPresetId(id)) throw SavedSearchNotFoundException()
        val entity = savedSearchRepository.findByIdOrNull(id) ?: throw SavedSearchNotFoundException()
        val canEditShared = permissions.contains(sharedEditKey)
        if (!isEditable(entity, employeeId, canEditShared)) {
            throw SavedSearchForbiddenException()
        }
        // 이름 변경 시 유니크 충돌 검사 (자기 자신 제외). owner 유무에 따라 분기.
        if (entity.name != request.name) {
            val ownerId = entity.ownerId
            val duplicate = if (ownerId != null) {
                savedSearchRepository.existsByResourceKeyAndOwnerIdAndScopeAndName(
                    entity.resourceKey, ownerId, entity.scope, request.name,
                )
            } else {
                savedSearchRepository.existsByResourceKeyAndOwnerIdIsNullAndScopeAndName(
                    entity.resourceKey, entity.scope, request.name,
                )
            }
            if (duplicate) {
                throw SavedSearchDuplicateNameException()
            }
        }
        entity.name = request.name
        entity.filters = request.filters
        entity.sortOrder = request.sortOrder
        return toResponse(entity, employeeId, canEditShared)
    }

    @Transactional
    fun delete(id: Long, employeeId: Long, permissions: Set<String>) {
        if (SavedSearchPresets.isSystemPresetId(id)) throw SavedSearchNotFoundException()
        val entity = savedSearchRepository.findByIdOrNull(id) ?: throw SavedSearchNotFoundException()
        if (!isEditable(entity, employeeId, permissions.contains(sharedEditKey))) {
            throw SavedSearchForbiddenException()
        }
        savedSearchRepository.delete(entity)
    }

    /** PRIVATE = 소유자 본인 / SHARED = saved_search EDIT 권한 보유 시 수정·삭제 가능. */
    private fun isEditable(entity: SavedSearch, employeeId: Long, canEditShared: Boolean): Boolean =
        when (entity.scope) {
            SavedSearchScope.PRIVATE -> entity.ownerId == employeeId
            SavedSearchScope.SHARED -> canEditShared
        }

    private fun requireSharedEditPermission(permissions: Set<String>) {
        if (!permissions.contains(sharedEditKey)) {
            throw SavedSearchForbiddenException()
        }
    }

    private fun toResponse(entity: SavedSearch, employeeId: Long, canEditShared: Boolean): SavedSearchResponse {
        val ownerName = entity.ownerId?.let { employeeRepository.findByIdOrNull(it)?.name }
        return SavedSearchResponse.of(entity, ownerName, isEditable(entity, employeeId, canEditShared))
    }

    private fun resolveOwnerNames(ownerIds: List<Long?>): Map<Long, String> {
        val nonNull = ownerIds.filterNotNull().distinct()
        if (nonNull.isEmpty()) return emptyMap()
        return employeeRepository.findAllById(nonNull)
            .associate { it.id to it.name }
    }
}
