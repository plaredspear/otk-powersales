package com.otoki.powersales.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SF PermissionSet 의 object/system 권한 비트 (spec #782 P1-B).
 *
 * objectPermissions 는 SObject 단위 권한 — `{ "Account": { "viewAllRecords": true, ... }, ... }` jsonb 박제.
 * application layer 에서 Jackson 으로 Map<String, Map<String, Boolean>> ↔ JSON 직렬화.
 */
@Entity
@Table(name = "permission_set_flags")
class PermissionSetFlags(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_set_flags_id")
    val id: Long = 0,

    @Column(name = "permission_set_sfid", nullable = false, length = 18, unique = true)
    var permissionSetSfid: String,

    @Column(name = "permission_set_name", nullable = false, length = 80)
    var permissionSetName: String,

    @Column(name = "permissions_view_all_data", nullable = false)
    var permissionsViewAllData: Boolean = false,

    @Column(name = "permissions_modify_all_data", nullable = false)
    var permissionsModifyAllData: Boolean = false,

    // PG 운영은 V175 의 jsonb 컬럼 정의 사용. entity 측 columnDefinition 미명시 — H2 호환 + Hibernate 자동 매핑.
    @Column(name = "object_permissions")
    var objectPermissions: String? = null,
)
