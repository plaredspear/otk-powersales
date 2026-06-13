package com.otoki.powersales.platform.auth.sharing.dto

/**
 * User 의 활성 PermissionSet 권한 평가 결과 immutable snapshot (spec #782 P2-B).
 *
 * `viewAllRecordsBySObject` / `modifyAllRecordsBySObject` 는 SObject API name 별 boolean.
 * 본 PermissionSet 의 ObjectPermission JSON 을 합산한 결과.
 *
 * `viewAllDataSystem` / `modifyAllDataSystem` 는 PermissionSet 의 시스템 권한 비트
 * (`PermissionsViewAllData` / `PermissionsModifyAllData`) OR 합산.
 */
data class PermissionSetSnapshot(
    val viewAllDataSystem: Boolean,
    val modifyAllDataSystem: Boolean,
    val viewAllRecordsBySObject: Map<String, Boolean>,
    val modifyAllRecordsBySObject: Map<String, Boolean>,
    /**
     * 사용자가 부여받은 PermissionSet 의 정규 id 집합 (spec #796).
     *
     * `permission_set_flags.permission_set_id` 를 통해 매핑된 정규 [permission_set.permission_set_id] 만 수집.
     * Stage2 fk resolve 가 채우지 않은 PermissionSet (운영 미적재 또는 외부 추가) 은 본 집합에서 제외.
     *
     * RecordTypePermissionEvaluator / FlsService 가 본 집합으로 PermissionSet × RT/FLS lookup.
     */
    val permissionSetIds: Set<Long>,
) {
    fun hasViewAllRecords(sObjectName: String): Boolean =
        viewAllDataSystem || (viewAllRecordsBySObject[sObjectName] == true)

    fun hasModifyAllRecords(sObjectName: String): Boolean =
        modifyAllDataSystem || (modifyAllRecordsBySObject[sObjectName] == true)

    companion object {
        val NONE = PermissionSetSnapshot(
            viewAllDataSystem = false,
            modifyAllDataSystem = false,
            viewAllRecordsBySObject = emptyMap(),
            modifyAllRecordsBySObject = emptyMap(),
            permissionSetIds = emptySet(),
        )
    }
}
