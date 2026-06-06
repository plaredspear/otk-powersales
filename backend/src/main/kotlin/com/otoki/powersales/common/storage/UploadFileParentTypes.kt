package com.otoki.powersales.common.storage

/**
 * UploadFile.parentType 의 SF Object 명 SoT.
 *
 * Stage1 마이그레이션은 SF UploadFile__c.Object__c 를 `parent_type` 컬럼에 그대로 직복하므로
 * 본 상수는 항상 SF 원형 (예: "DKRetail__Claim__c") 을 사용한다. 신규 INSERT 경로
 * (NoticeService / SuggestionService 등) 도 같은 값을 사용하여 마이그레이션 row 와 신규 row 의
 * `parent_type` 값이 정합한다.
 *
 * Stage2 polymorphic-parent substep 이 `(parent_type, record_sfid)` → `parent_id` 변환 시
 * [PARENT_TYPE_TO_TABLE] 를 참조.
 */
object UploadFileParentTypes {
    const val CLAIM: String = "DKRetail__Claim__c"
    const val NOTICE: String = "DKRetail__Notice__c"
    const val SUGGESTION: String = "DKRetail__Proposal__c"
    const val SITE_ACTIVITY: String = "DKRetail__SiteAcitivity__c"
}

/**
 * Stage2 polymorphic-parent resolver 가 참조하는 매핑 표.
 *
 *   SF Object 명 → (신규 entity table, 신규 entity PK 컬럼)
 *
 * stage2 가 한 entry 당 한 UPDATE 실행:
 * ```sql
 * UPDATE powersales.upload_file uf
 * SET parent_id = c.<idColumn>
 * FROM powersales.<table> c
 * WHERE uf.parent_type = '<sfObject>' AND uf.record_sfid = c.sfid AND uf.parent_id IS NULL;
 * ```
 *
 * 신규 polymorphic 대상 entity 추가 시 본 표에 1줄 추가.
 */
internal data class PolymorphicParentSpec(
    val refTable: String,
    val refIdColumn: String,
)

internal val UPLOAD_FILE_POLYMORPHIC_PARENTS: Map<String, PolymorphicParentSpec> = linkedMapOf(
    UploadFileParentTypes.CLAIM to PolymorphicParentSpec("claim", "claim_id"),
    UploadFileParentTypes.NOTICE to PolymorphicParentSpec("notice", "notice_id"),
    UploadFileParentTypes.SUGGESTION to PolymorphicParentSpec("suggestion", "suggestion_id"),
    UploadFileParentTypes.SITE_ACTIVITY to PolymorphicParentSpec("site_activity", "site_activity_id"),
)
