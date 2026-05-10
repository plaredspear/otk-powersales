package com.otoki.powersales.account.service

import com.otoki.powersales.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository

/**
 * [Organization] 폴백 lookup.
 *
 * - 캐시 키: [Organization.costCenterLevel5] → [Organization.costCenterLevel4] → [Organization.costCenterLevel3] 의 deepest non-blank.
 * - 매칭 우선순위: [AccountUpsertCommand.branchCode] → [AccountUpsertCommand.salesDeptCode] → [AccountUpsertCommand.divisionCode].
 *
 * 호출 단위 캐시이므로 빈으로 등록하지 않는다 — [build] 정적 팩토리로 [AccountUpsertService.upsert] 1회 호출당 1개 인스턴스.
 */
class OrganizationLookup private constructor(
    private val cache: Map<String, Organization>
) {

    fun match(command: AccountUpsertCommand): Organization? {
        val branch = command.branchCode?.takeIf { it.isNotBlank() }
        if (branch != null) cache[branch]?.let { return it }
        val salesDept = command.salesDeptCode?.takeIf { it.isNotBlank() }
        if (salesDept != null) cache[salesDept]?.let { return it }
        val division = command.divisionCode?.takeIf { it.isNotBlank() }
        if (division != null) cache[division]?.let { return it }
        return null
    }

    companion object {
        fun build(repository: OrganizationRepository): OrganizationLookup {
            val cache = mutableMapOf<String, Organization>()
            repository.findAll().forEach { org ->
                val key = sequenceOf(
                    org.costCenterLevel5,
                    org.costCenterLevel4,
                    org.costCenterLevel3
                ).firstOrNull { !it.isNullOrBlank() } ?: return@forEach
                cache.putIfAbsent(key, org)
            }
            return OrganizationLookup(cache)
        }
    }
}
