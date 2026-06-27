package com.otoki.powersales.savedsearch.repository

import com.otoki.powersales.savedsearch.entity.QSavedSearch.Companion.savedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import com.querydsl.jpa.impl.JPAQueryFactory

class SavedSearchRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : SavedSearchRepositoryCustom {

    override fun findVisible(resourceKey: String, ownerId: Long): List<SavedSearch> {
        return queryFactory
            .selectFrom(savedSearch)
            .where(
                savedSearch.resourceKey.eq(resourceKey),
                // (전체 SHARED) ∪ (본인 소유 PRIVATE)
                savedSearch.scope.eq(SavedSearchScope.SHARED)
                    .or(savedSearch.ownerId.eq(ownerId)),
            )
            .orderBy(savedSearch.sortOrder.asc(), savedSearch.createdAt.asc())
            .fetch()
    }
}
