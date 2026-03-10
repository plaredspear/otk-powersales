package com.otoki.internal.education.dto.response

/**
 * 교육 자료 작성/수정 응답 DTO
 */
data class EducationMutationResponse(
    val eduId: String,
    val eduTitle: String,
    val eduContent: String,
    val eduCode: String,
    val eduCodeNm: String,
    val empCode: String,
    val instDate: String,
    val updDate: String?,
    val attachments: List<AttachmentInfo>
)

/**
 * 첨부파일 정보
 */
data class AttachmentInfo(
    val eduFileKey: String,
    val eduFileType: String,
    val eduFileOrgNm: String
)
