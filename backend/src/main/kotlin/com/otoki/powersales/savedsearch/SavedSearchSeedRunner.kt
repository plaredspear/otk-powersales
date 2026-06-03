package com.otoki.powersales.savedsearch

import com.otoki.powersales.savedsearch.entity.SavedSearch
import com.otoki.powersales.savedsearch.entity.SavedSearchScope
import com.otoki.powersales.savedsearch.repository.SavedSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * 저장된 검색 기본 공용 프리셋 부팅 sync (Spec #852 v1.3).
 *
 * 첫 배포 직후 공용 프리셋이 0개라 드롭다운이 비는 것을 막기 위해, 화면별 기본 SHARED 프리셋이
 * 없으면 1개 생성한다. owner 없는 시스템 프리셋(owner_id = null).
 *
 * ## 설계 (CLAUDE.md §4 reference data 정책 정합 — Flyway INSERT 금지)
 * - **ensure-exists 멱등**: 이미 존재하면 건드리지 않는다. 운영자가 이름/필터를 수정·삭제해도 덮어쓰지 않음.
 * - **최소 시드**: "전체 행사 조회"(빈 필터) 1개만 보장. 그 외 공용 프리셋은 운영 UI 로 관리(동적).
 */
@Component
@Order(100)
@Profile("!test")
class SavedSearchSeedRunner(
    private val repository: SavedSearchRepository,
    private val transactionTemplate: TransactionTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    /** (resourceKey, name, filters) 기본 공용 프리셋 시드 정의. */
    private val seeds = listOf(
        SeedPreset(resourceKey = "promotion", name = "전체 행사 조회", filters = emptyMap()),
    )

    override fun run(args: ApplicationArguments) {
        transactionTemplate.executeWithoutResult {
            var inserted = 0
            for (seed in seeds) {
                val exists = repository.existsByResourceKeyAndOwnerIdIsNullAndScopeAndName(
                    resourceKey = seed.resourceKey,
                    scope = SavedSearchScope.SHARED,
                    name = seed.name,
                )
                if (!exists) {
                    repository.save(
                        SavedSearch(
                            resourceKey = seed.resourceKey,
                            name = seed.name,
                            scope = SavedSearchScope.SHARED,
                            ownerId = null,
                            filters = seed.filters,
                            sortOrder = 0,
                        ),
                    )
                    inserted++
                }
            }
            if (inserted > 0) {
                log.info("[SavedSearchSeedRunner] 기본 공용 프리셋 {}건 생성", inserted)
            }
        }
    }

    private data class SeedPreset(
        val resourceKey: String,
        val name: String,
        val filters: Map<String, Any?>,
    )
}
