package com.otoki.powersales.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

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

    // PG = jsonb (V175), H2 = JSON-as-text. Hibernate 6+ dialect 자동 매핑 (SapOutbox.payload / ScheduledJobRun.metadata 와 동일 패턴).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "object_permissions")
    var objectPermissions: String? = null,
)
