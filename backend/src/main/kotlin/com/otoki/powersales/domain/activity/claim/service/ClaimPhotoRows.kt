package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.storage.UploadFileKbnTypes
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import org.springframework.web.multipart.MultipartFile

/**
 * 등록된 claim 에 첨부할 사진 [UploadFile] row 빌드 — web · mobile 등록 진입점 공용.
 *
 * defect/part 필수, receipt 선택. part == 모바일 "label(라벨)" 사진 == SF payload 의 PartImage*
 * (uploadKbn=CLAIM_PART). 진입점이 S3 업로드를 마친 (file, key) 쌍을 넘기면 claim PK 로 연결한 row 를 만든다.
 */
fun buildClaimPhotoRows(
    claim: Claim,
    defectPhoto: MultipartFile,
    defectKey: String,
    partPhoto: MultipartFile,
    partKey: String,
    receiptPhoto: MultipartFile?,
    receiptKey: String?,
): List<UploadFile> = buildList {
    add(buildClaimPhotoRow(claim, defectPhoto, defectKey, UploadFileKbnTypes.CLAIM_DEFECT))
    add(buildClaimPhotoRow(claim, partPhoto, partKey, UploadFileKbnTypes.CLAIM_PART))
    if (receiptPhoto != null && receiptKey != null) {
        add(buildClaimPhotoRow(claim, receiptPhoto, receiptKey, UploadFileKbnTypes.CLAIM_RECEIPT))
    }
}

private fun buildClaimPhotoRow(claim: Claim, file: MultipartFile, key: String, uploadKbn: String): UploadFile =
    UploadFile(
        name = file.originalFilename ?: "unknown",
        uniqueKey = key,
        fileSize = file.size.toString(),
        parentType = UploadFileParentTypes.CLAIM,
        parentId = claim.id,
        uploadKbn = uploadKbn,
        isDeleted = false,
    )
