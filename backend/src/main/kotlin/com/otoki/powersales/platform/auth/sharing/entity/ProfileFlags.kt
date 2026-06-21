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
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * SF Profile 의 system 권한 비트 + 객체/가상자원 권한 (spec #782 P1-B).
 *
 * Profile entity (#780) 는 SF describe 8 필드만 보존 — system 권한 비트 (`PermissionsViewAllData` 등 5종)
 * 는 본 테이블로 외부화. Profile 의 사실상 immutable 성격 유지 + 운영 정책 변경 시 본 테이블만 갱신.
 *
 * SF 레거시에서 Profile 은 ObjectPermission 을 보유하며 발령 시 User.ProfileId 가 자동 결정되어
 * "직책 → 화면 권한" 이 Profile 로 자동 전파된다. 본 entity 의 [objectPermissions] / [customPermissions] 가
 * 그 슬롯으로, PermissionSetFlags 와 동일 JSON 구조 ({자원명 → {allowRead, allowCreate, allowEdit, allowDelete}}).
 *
 * profile_id UNIQUE — 한 Profile 당 1행. XML 출처: `profiles/<Name>.profile-meta.xml` 의 systemPermissions / objectPermissions.
 */
@DomainName("프로파일 플래그")
@Entity
@SFMeta(SFMetaSource.PROFILE_XML, "systemPermissions")
@Table(name = "profile_flags")
class ProfileFlags(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_flags_id")
    val id: Long = 0,

    @Column(name = "profile_id", nullable = false, unique = true)
    var profileId: Long,

    @Column(name = "permissions_view_all_data", nullable = false)
    var permissionsViewAllData: Boolean = false,

    @Column(name = "permissions_modify_all_data", nullable = false)
    var permissionsModifyAllData: Boolean = false,

    @Column(name = "permissions_view_all_users", nullable = false)
    var permissionsViewAllUsers: Boolean = false,

    @Column(name = "permissions_manage_users", nullable = false)
    var permissionsManageUsers: Boolean = false,

    @Column(name = "permissions_api_enabled", nullable = false)
    var permissionsApiEnabled: Boolean = false,

    // SF API name → {allowRead, allowCreate, allowEdit, allowDelete} JSON. PermissionSetFlags 와 동일 구조.
    @Column(name = "object_permissions", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var objectPermissions: String? = null,

    // 가상 자원 (@PermissionResource) → 동일 4비트 JSON. SF API name 아님.
    @Column(name = "custom_permissions", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var customPermissions: String? = null,

    // Web admin 에서 권한 비트를 편집하면 set. Stage2 재적재 시 dirty row 는 skip (SF 덮어쓰기 보호).
    @Column(name = "is_locally_modified", nullable = false)
    var isLocallyModified: Boolean = false,
)
