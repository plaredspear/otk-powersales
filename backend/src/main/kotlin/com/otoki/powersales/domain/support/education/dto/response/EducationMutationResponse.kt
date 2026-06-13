package com.otoki.powersales.domain.support.education.dto.response

import java.time.LocalDateTime

/**
 * 교육 자료 작성/수정 응답 DTO
 */
data class EducationMutationResponse(
    val eduId: String,
    val eduTitle: String,
    val eduContent: String,
    val eduCode: String,
    val eduCodeNm: String,
    val employeeId: Long?,
    val instDate: LocalDateTime?,
    val updDate: LocalDateTime?,
    val attachments: List<AttachmentInfo>
)

/**
 * 첨부파일 정보
 */
data class AttachmentInfo(
    val fileKey: String,
    val fileType: String,
    val fileOriginalName: String
)
