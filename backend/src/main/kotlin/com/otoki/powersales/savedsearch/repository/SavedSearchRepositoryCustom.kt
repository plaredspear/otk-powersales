package com.otoki.powersales.savedsearch.repository

import com.otoki.powersales.savedsearch.entity.SavedSearch

interface SavedSearchRepositoryCustom {

    /**
     * 목록 조회 — (본인 소유 PRIVATE) ∪ (전체 SHARED). sortOrder ASC, createdAt ASC 정렬.
     */
    fun findVisible(resourceKey: String, ownerId: Long): List<SavedSearch>
}
