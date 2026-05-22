package com.otoki.powersales.auth.sharing.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SF Profile 의 system 권한 비트 (spec #782 P1-B).
 *
 * Profile entity (#780) 는 SF describe 8 필드만 보존 — system 권한 비트 (`PermissionsViewAllData` 등 5종)
 * 는 본 테이블로 외부화. Profile 의 사실상 immutable 성격 유지 + 운영 정책 변경 시 본 테이블만 갱신.
 *
 * profile_id UNIQUE — 한 Profile 당 1행.
 */
@Entity
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
)
