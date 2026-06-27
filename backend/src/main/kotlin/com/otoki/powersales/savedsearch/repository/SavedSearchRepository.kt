package com.otoki.powersales.savedsearch.repository

import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import org.springframework.data.jpa.repository.JpaRepository

interface SavedSearchRepository : JpaRepository<SavedSearch, Long>, SavedSearchRepositoryCustom {

    /** 유니크 제약 (resourceKey, ownerId, scope, name) 사전 검증용. */
    fun existsByResourceKeyAndOwnerIdAndScopeAndName(
        resourceKey: String,
        ownerId: Long,
        scope: SavedSearchScope,
        name: String,
    ): Boolean

    /** 시스템 기본 공용 프리셋(owner 없는 SHARED) 존재 확인용 — 부팅 sync 멱등 처리. */
    fun existsByResourceKeyAndOwnerIdIsNullAndScopeAndName(
        resourceKey: String,
        scope: SavedSearchScope,
        name: String,
    ): Boolean
}
