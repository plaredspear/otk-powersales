package com.otoki.powersales.platform.common.salesforce

/**
 * SF 메타파일 / 메타 API 의 element 를 정규화한 entity 의 권위 출처 마커.
 *
 * SF sObject 1:1 매핑 (`@SFObject`) 도 신규 시스템 자체 생성물 (`@SFShareAux`) 도 아닌,
 * SF 메타파일 (permission-set / profile / sharing-rules / record-type / object-meta /
 * settings XML) 또는 describe API 의 특정 element 본문을 1행씩 적재한 entity 에 부착.
 *
 * 한 entity 가 둘 이상의 (source, element) pair 에서 정규화되어 들어오는 경우
 * `@Repeatable` 로 여러 번 부착한다.
 *
 * 예 — 단일 출처:
 * ```
 * @SFMeta(SFMetaSource.PERMISSION_SET_XML, "fieldPermissions")
 * class PermissionSetFieldPermission { ... }
 * ```
 *
 * 예 — 복수 출처 (SObjectSetting):
 * ```
 * @SFMeta(SFMetaSource.OBJECT_META_XML, "sharingModel")
 * @SFMeta(SFMetaSource.SETTINGS_XML, "sharingSettings")
 * @SFMeta(SFMetaSource.SETTINGS_XML, "sharingHierarchy")
 * class SObjectSetting { ... }
 * ```
 *
 * reflection 으로 전체 열거: `clazz.annotations.filterIsInstance<SFMeta>()`.
 */
@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SFMeta(val source: SFMetaSource, val element: String = "")
