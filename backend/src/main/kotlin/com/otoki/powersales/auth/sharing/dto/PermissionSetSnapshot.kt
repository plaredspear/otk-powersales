package com.otoki.powersales.auth.sharing.dto

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
        )
    }
}
