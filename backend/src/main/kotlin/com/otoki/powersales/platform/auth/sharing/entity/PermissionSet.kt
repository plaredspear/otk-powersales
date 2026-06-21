package com.otoki.powersales.platform.auth.sharing.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFObject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * SF PermissionSet 정규 테이블 (spec #796).
 *
 * `permission_set_flags` 는 "객체 권한 비트 (objectPermissions JSONB)" 만 보관하는 역할이고,
 * 본 entity 는 PermissionSet 자체 (name / label / description 메타). 둘은 1:1 관계.
 *
 * - `permission_set_record_type.permission_set_id` / `permission_set_field_permission.permission_set_id`
 *   가 본 entity 의 PK 를 참조.
 * - 적재 출처: extract-csv.sh `PS_SOQL` — `SELECT Id, Name, Label FROM PermissionSet WHERE IsCustom = TRUE`.
 *   XML 출처 (`permissionsets/<Name>.permissionset-meta.xml`) 는 PermissionSetFlags / PermissionSetRecordType /
 *   PermissionSetFieldPermission 가 분담.
 */
@DomainName("권한집합")
@Entity
@SFObject("PermissionSet")
@Table(name = "permission_set")
class PermissionSet(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("권한집합ID")
    @Column(name = "permission_set_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @FieldName("이름")
    @Column(name = "name", nullable = false, length = 80, unique = true)
    var name: String,

    @FieldName("라벨")
    @Column(name = "label", length = 255)
    var label: String? = null,

    @FieldName("행사대체제품")
    @Column(name = "description", length = 1024)
    var description: String? = null,
) : BaseEntity()
