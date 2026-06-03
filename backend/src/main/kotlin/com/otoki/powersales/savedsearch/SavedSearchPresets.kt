package com.otoki.powersales.savedsearch

import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope

/**
 * 저장된 검색 기본 공용 프리셋 SoT (Spec #852).
 *
 * "현 시점에 어떤 기본 공용 프리셋이 있어야 하는가" 의 declarative reference data 는 DB row 가 아니라
 * 본 코드 상수가 단일 진실(Single source of truth)이다 — CLAUDE.md §4 reference data 정책 정합
 * (Flyway INSERT 금지 / SoT object 패턴). [RolePermissionMatrix] 와 동일한 접근.
 *
 * ## 동작
 * - DB 에 저장하지 않는다. [SavedSearchService.list] 응답에 음수 [id] 로 합성해 끼워넣는다.
 * - 운영자 수정/삭제 불가 (editable=false). 분기별 변경은 본 상수 수정 후 배포로 모든 환경에 즉시 반영.
 * - 따라서 "삭제해도 재부팅 시 재생성" 같은 DB row 와 코드 간 drift 가 원천적으로 없다.
 *
 * ## id 규약
 * 음수 id (-1, -2, ...) 를 부여한다. DB IDENTITY 는 양수만 발급하므로 충돌하지 않으며,
 * 프런트의 update/delete 호출이 들어와도 음수 id 는 DB 에 없어 404 로 안전하게 거부된다.
 */
object SavedSearchPresets {

    /** resourceKey 별 기본 공용 프리셋. 같은 resourceKey 안에서 정의 순서대로 sortOrder 0,1,... 부여. */
    private val presets: Map<String, List<Preset>> = mapOf(
        "promotion" to listOf(
            Preset(name = "전체 행사 조회", filters = emptyMap()),
        ),
    )

    /**
     * 주어진 화면(resourceKey)의 기본 공용 프리셋을 [SavedSearch] 엔티티로 합성해 반환한다.
     * id 는 -1 부터 내림차순, scope=SHARED, ownerId=null. **DB 에 저장하지 않는 in-memory 엔티티.**
     */
    fun systemPresetsFor(resourceKey: String): List<SavedSearch> {
        val list = presets[resourceKey] ?: return emptyList()
        return list.mapIndexed { index, preset ->
            SavedSearch(
                resourceKey = resourceKey,
                name = preset.name,
                scope = SavedSearchScope.SHARED,
                ownerId = null,
                filters = preset.filters,
                sortOrder = index,
                id = -(index + 1).toLong(),
            )
        }
    }

    /** 해당 id 가 시스템 프리셋(음수 id)인지. */
    fun isSystemPresetId(id: Long): Boolean = id < 0

    private data class Preset(
        val name: String,
        val filters: Map<String, Any?>,
    )
}
