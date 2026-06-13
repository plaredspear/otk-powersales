package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.repository.PermissionSetFieldPermissionRepository
import com.otoki.powersales.platform.auth.sharing.repository.ProfileFieldPermissionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * FLS (Field-Level Security) 평가 (spec #795).
 *
 * Profile + PermissionSet 합산하여 사용자별 가시 field 집합 산출.
 *
 * ## 결정 사항 정합
 * - Q1 옵션 1: 점진 도입 — `@FlsFiltered` 부착 endpoint 만 평가
 * - Q2 옵션 1: omit — readable=false field 는 응답 JSON 에서 제거 (SF 동등)
 * - Q3 옵션 1: read 만 본 spec — write FLS 는 후행 spec
 * - Q4 옵션 1: audit field 면제 — `@FlsField` 부착 안 한 field 는 항상 통과
 */
@Service
class FlsService(
    private val profileFieldPermissionRepository: ProfileFieldPermissionRepository,
    private val permissionSetFieldPermissionRepository: PermissionSetFieldPermissionRepository,
) {

    /**
     * 사용자의 sObject 별 가시 field name 집합. Profile + PermissionSet 합산.
     *
     * 본 메서드는 `readable=true` 인 field 의 SF API name 만 반환. `@FlsField.sObjectField` 의
     * `.` split 후 sObject 매칭. SF SObject.Field 형식 그대로 사용 (Account.AnnualRevenue).
     */
    @Cacheable(cacheNames = ["field-permission:v1"], key = "#userId + ':' + #sObjectName")
    fun readableFields(userId: Long, sObjectName: String, profileId: Long?, permissionSetIds: Set<Long>): Set<String> {
        val result = mutableSetOf<String>()

        if (profileId != null) {
            profileFieldPermissionRepository.findAllByProfileIdAndSObjectName(profileId, sObjectName)
                .filter { it.readable }
                .forEach { result.add(it.fieldName) }
        }

        permissionSetIds.forEach { psId ->
            permissionSetFieldPermissionRepository.findAllByPermissionSetIdAndSObjectName(psId, sObjectName)
                .filter { it.readable }
                .forEach { result.add(it.fieldName) }
        }

        return result
    }

    /**
     * 사용자의 sObject 별 editable field name 집합 — write FLS 검사용. 본 spec 은 read 만 (Q3 옵션 1)이라
     * 현재 구현은 추후 spec 책임. 본 메서드는 placeholder — 빈 set return.
     */
    fun editableFields(userId: Long, sObjectName: String, profileId: Long?, permissionSetIds: Set<Long>): Set<String> {
        // TODO: write FLS 후행 spec 도입 시 readableFields 동등 로직.
        return emptySet()
    }
}
