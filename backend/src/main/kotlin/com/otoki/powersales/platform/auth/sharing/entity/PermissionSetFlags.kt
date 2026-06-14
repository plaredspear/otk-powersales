package com.otoki.powersales.platform.auth.sharing.entity

import com.otoki.powersales.platform.common.salesforce.SFMeta
import com.otoki.powersales.platform.common.salesforce.SFMetaSource
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
 *
 * XML 출처: `permissionsets/<Name>.permissionset-meta.xml` 의 `<objectPermissions>` + system 권한 비트.
 */
@Entity
@SFMeta(SFMetaSource.PERMISSION_SET_XML, "objectPermissions")
@Table(name = "permission_set_flags")
class PermissionSetFlags(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_set_flags_id")
    val id: Long = 0,

    // V197 — Stage1 적재 시점 NULL 허용 (XML 메타 출처라 sfid 부재). Stage2 fk-natural-key substep 의
    // resolvePermissionSetFlagsSfid() 가 permission_set.sfid 로 채움. UNIQUE 는 partial UNIQUE INDEX
    // (WHERE permission_set_sfid IS NOT NULL) 로 V197 에서 전환됨 — entity unique 표현 불가하므로 제거.
    @Column(name = "permission_set_sfid", length = 18)
    var permissionSetSfid: String? = null,

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

    // spec #808 — JPA entity 가 없는 가상 자원 (@PermissionResource) 의 CRUD 비트. SF customPermissions 정합.
    // 예: { "dashboard": { "allowRead": true } }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_permissions")
    var customPermissions: String? = null,

    // spec #796 — permission_set 정규 테이블 FK (Stage2 fk resolve 후 채움)
    @Column(name = "permission_set_id")
    var permissionSetId: Long? = null,

    // spec #837 — dirty 플래그. SF 출처 (sfid 보유 PS 의 flags) 가 신규 시스템에서 수정되면 true.
    // Stage1 재적재 시 본 컬럼 true 인 행은 보존 정책 (재적재 service 변경은 후속 — 현재는 audit 컬럼만).
    @Column(name = "is_locally_modified", nullable = false)
    var isLocallyModified: Boolean = false,
)
