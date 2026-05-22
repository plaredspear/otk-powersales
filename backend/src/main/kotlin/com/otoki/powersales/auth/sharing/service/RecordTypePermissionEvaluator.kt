package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.sharing.repository.PermissionSetRecordTypeRepository
import com.otoki.powersales.auth.sharing.repository.ProfileRecordTypeRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * RecordType 권한 평가 (spec #794).
 *
 * Profile + PermissionSet 합산하여 사용자별 가시 RecordType id 집합 산출.
 *
 * ## 결정 사항 정합
 * - Q1 옵션 1 — entity 의 `record_type_id IS NULL` row 는 evaluator 분기 자동 통과 (SF 동등)
 * - Q2 옵션 1 — 가시 RT 0건 시 sObject 의 `record_type_id IS NOT NULL` row 모두 차단 (SF 동등)
 * - Q3 옵션 1 — cache evict 는 #792 의 sharing recalc 그룹에 통합
 * - Q4 옵션 1 — Master RT 적재 안 함 (`record_type_id IS NULL` 이 곧 Master 의미)
 */
@Service
class RecordTypePermissionEvaluator(
    private val profileRecordTypeRepository: ProfileRecordTypeRepository,
    private val permissionSetRecordTypeRepository: PermissionSetRecordTypeRepository,
) {

    /**
     * 사용자의 가시 record_type_id 집합. Profile + PermissionSet 합산.
     *
     * 본 메서드는 sObject 무관 — 전 sObject 통합 결과. evaluator predicate 합성 시 sObject 별
     * filter 는 별도 처리 (`profile_record_type.sobject_name` 매칭).
     */
    @Cacheable(cacheNames = ["record-type-visibility:v1"], key = "#userId")
    fun visibleRecordTypeIds(userId: Long, profileId: Long?, permissionSetIds: Set<Long>): Set<Long> {
        val result = mutableSetOf<Long>()

        // Profile 측 (운영 0건 — 빈 결과 일반적)
        if (profileId != null) {
            profileRecordTypeRepository.findAllByProfileId(profileId)
                .filter { it.visible && it.recordTypeId != null }
                .forEach { result.add(it.recordTypeId!!) }
        }

        // PermissionSet 측 (운영 10건)
        permissionSetIds.forEach { psId ->
            permissionSetRecordTypeRepository.findAllByPermissionSetId(psId)
                .filter { it.visible && it.recordTypeId != null }
                .forEach { result.add(it.recordTypeId!!) }
        }

        return result
    }
}
