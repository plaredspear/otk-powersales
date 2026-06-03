package com.otoki.powersales.savedsearch.repository

import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SavedSearchRepository : JpaRepository<SavedSearch, Long> {

    /**
     * 목록 조회 — (본인 소유 PRIVATE) ∪ (전체 SHARED). sortOrder ASC, createdAt ASC 정렬.
     */
    @Query(
        """
        SELECT s FROM SavedSearch s
        WHERE s.resourceKey = :resourceKey
          AND (s.scope = com.otoki.powersales.savedsearch.entity.SavedSearchScope.SHARED
               OR s.ownerId = :ownerId)
        ORDER BY s.sortOrder ASC, s.createdAt ASC
        """
    )
    fun findVisible(
        @Param("resourceKey") resourceKey: String,
        @Param("ownerId") ownerId: Long,
    ): List<SavedSearch>

    /** 유니크 제약 (resourceKey, ownerId, scope, name) 사전 검증용. */
    fun existsByResourceKeyAndOwnerIdAndScopeAndName(
        resourceKey: String,
        ownerId: Long,
        scope: SavedSearchScope,
        name: String,
    ): Boolean
}
