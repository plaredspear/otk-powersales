package com.otoki.powersales.platform.common.salesforce

/**
 * `@SFMeta` 의 출처 카테고리.
 *
 * SF sObject 본체가 아니라 메타파일/메타 API 의 element 를 정규화한 entity 의 권위 출처 구분.
 */
enum class SFMetaSource {
    /** `permissionsets/<Name>.permissionset-meta.xml` */
    PERMISSION_SET_XML,

    /** `profiles/<Name>.profile-meta.xml` */
    PROFILE_XML,

    /** `sharingRules/<SObject>.sharingRules-meta.xml` */
    SHARING_RULES_XML,

    /** `objects/<SObject>/recordTypes/<DeveloperName>.recordType-meta.xml` */
    RECORD_TYPE_XML,

    /** `objects/<SObject>/<SObject>.object-meta.xml` (Custom SObject 의 sharingModel 등) */
    OBJECT_META_XML,

    /** `settings/Sharing.settings-meta.xml` (Standard SObject 의 OWD / sharingHierarchy) */
    SETTINGS_XML,

    /** SF describe API (`childRelationships` 등 운영 실측 메타) */
    DESCRIBE_API,
}
