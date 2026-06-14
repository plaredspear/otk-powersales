package com.otoki.powersales.platform.common.storage

/**
 * UploadFile.uploadKbn 의 도메인별 구분 값 SoT.
 *
 * SF claim 도메인 (`IF_REST_MOBILE_ClaimRegist`) 은 ContentVersion.customName__c 에
 * `claim_` / `part_` / `receipt_` prefix 로 사진 자리를 구분한다. 신규 시스템은
 * UploadFile.uploadKbn 에 prefix 의 어간 ("claim" / "part" / "receipt") 만 저장하여
 * SF 패턴과 정합한다.
 *
 * Resend 흐름이 이 값으로 사진 3장을 식별하여 SF `/ClaimRegist` API 의
 * ClaimImageBuffer / PartImageBuffer / ReceiptImageBuffer 자리에 정확히 매핑한다.
 */
object UploadFileKbnTypes {
    /** 불량 사진 — SF customName__c 의 `claim_` prefix 정합 */
    const val CLAIM_DEFECT: String = "claim"

    /** 일부인 사진 — SF customName__c 의 `part_` prefix 정합 */
    const val CLAIM_PART: String = "part"

    /** 영수증 사진 — SF customName__c 의 `receipt_` prefix 정합 */
    const val CLAIM_RECEIPT: String = "receipt"
}
