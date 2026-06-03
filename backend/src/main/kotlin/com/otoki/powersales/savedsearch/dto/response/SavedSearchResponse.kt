package com.otoki.powersales.savedsearch.dto.response

import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope

data class SavedSearchResponse(
    val id: Long,
    val resourceKey: String,
    val name: String,
    val scope: SavedSearchScope,
    val ownerId: Long,
    val ownerName: String?,
    val filters: Map<String, Any?>,
    val sortOrder: Int,
    /** 호출자가 본 검색을 수정/삭제할 수 있는지. 프런트 버튼 노출 제어용. */
    val editable: Boolean,
) {
    companion object {
        fun of(entity: SavedSearch, ownerName: String?, editable: Boolean): SavedSearchResponse =
            SavedSearchResponse(
                id = entity.id,
                resourceKey = entity.resourceKey,
                name = entity.name,
                scope = entity.scope,
                ownerId = entity.ownerId,
                ownerName = ownerName,
                filters = entity.filters,
                sortOrder = entity.sortOrder,
                editable = editable,
            )
    }
}
