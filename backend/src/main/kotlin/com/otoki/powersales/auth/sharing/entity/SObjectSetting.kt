package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFMeta
import com.otoki.powersales.common.salesforce.SFMetaSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * SF OWD (Org-Wide Default) + role hierarchy 옵트인 메타 (spec #791).
 *
 * XML 메타 3 출처 정규화:
 *   - Custom SObject 의 `<sharingModel>` (`objects/<SObject>__c/<SObject>__c.object-meta.xml`)
 *   - Standard SObject 의 `<sharingSettings>` (`settings/Sharing.settings-meta.xml`)
 *   - Hierarchy 옵트인 `<sharingHierarchy>` (위 settings 파일)
 *
 * `Stage1Targets` 의 ALL 맵에 등록되어 #790 의 sharing 메타 entity 와 동일 ETL 흐름.
 * EntityMetadata.sObjectName 은 null (XML 메타 출처 — #790 Q1 옵션 1 정합).
 */
@Entity
@SFMeta(SFMetaSource.OBJECT_META_XML, "sharingModel")
@SFMeta(SFMetaSource.SETTINGS_XML, "sharingSettings")
@SFMeta(SFMetaSource.SETTINGS_XML, "sharingHierarchy")
@Table(name = "sobject_setting")
class SObjectSetting(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sobject_setting_id")
    val id: Long = 0,

    @Column(name = "sobject_name", nullable = false, length = 80, unique = true)
    var sObjectName: String,

    @Column(name = "org_wide_default", nullable = false, length = 30)
    var orgWideDefault: String,

    @Column(name = "allow_hierarchy_grant", nullable = false)
    var allowHierarchyGrant: Boolean = true,

    @Column(name = "parent_sobject_name", length = 80)
    var parentSObjectName: String? = null,
) : BaseEntity()
