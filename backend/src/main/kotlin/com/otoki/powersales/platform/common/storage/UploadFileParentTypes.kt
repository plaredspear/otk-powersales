package com.otoki.powersales.platform.common.storage

/**
 * UploadFile.parentType 의 SoT — **신규 시스템 엔티티 클래스명** (PascalCase).
 *
 * 마이그레이션 row 와 신규 INSERT row 모두 본 상수값을 parent_type 에 사용한다.
 * SF 원본 Object__c (예: "DKRetail__Claim__c") 는 parent_type 이 아니라 별도 `object_type`
 * 컬럼에 보존하고, Stage2 polymorphic-parent substep 이 object_type → parent_type (엔티티명)
 * 으로 변환한다 ([UPLOAD_FILE_POLYMORPHIC_PARENTS] 의 sfObjectName ↔ 상수 매핑).
 *
 * 신규 INSERT 경로 (ClaimService / NoticeService / SuggestionService / SiteActivityService) 와
 * 조회 (findByParentType...) 가 모두 본 상수를 사용하여 값이 정합한다.
 */
object UploadFileParentTypes {
    const val CLAIM: String = "Claim"
    const val NOTICE: String = "Notice"
    const val SUGGESTION: String = "Suggestion"
    const val SITE_ACTIVITY: String = "SiteActivity"
}

/**
 * Stage2 polymorphic-parent resolver 가 참조하는 매핑 표.
 *
 *   parent_type (엔티티명 상수) → (SF Object 원형명, 신규 entity table, 신규 entity PK 컬럼)
 *
 * Stage2 가 한 entry 당:
 *  1. object_type (SF 원형) → parent_type (엔티티명) 변환:
 *     ```sql
 *     UPDATE powersales.upload_file
 *     SET parent_type = '<entityName>'
 *     WHERE object_type = '<sfObjectName>' AND parent_type = 'UNKNOWN';
 *     ```
 *  2. (parent_type, record_sfid) → parent_id 연결:
 *     ```sql
 *     UPDATE powersales.upload_file uf
 *     SET parent_id = c.<idColumn>
 *     FROM powersales.<table> c
 *     WHERE uf.parent_type = '<entityName>' AND uf.record_sfid = c.sfid AND uf.parent_id IS NULL;
 *     ```
 *
 * 신규 polymorphic 대상 entity 추가 시 본 표에 1줄 추가.
 */
internal data class PolymorphicParentSpec(
    val sfObjectName: String,
    val refTable: String,
    val refIdColumn: String,
)

internal val UPLOAD_FILE_POLYMORPHIC_PARENTS: Map<String, PolymorphicParentSpec> = linkedMapOf(
    UploadFileParentTypes.CLAIM to PolymorphicParentSpec("DKRetail__Claim__c", "claim", "claim_id"),
    UploadFileParentTypes.NOTICE to PolymorphicParentSpec("DKRetail__Notice__c", "notice", "notice_id"),
    UploadFileParentTypes.SUGGESTION to PolymorphicParentSpec("DKRetail__Proposal__c", "suggestion", "suggestion_id"),
    UploadFileParentTypes.SITE_ACTIVITY to PolymorphicParentSpec(
        "DKRetail__SiteAcitivity__c", "site_activity", "site_activity_id",
    ),
)
