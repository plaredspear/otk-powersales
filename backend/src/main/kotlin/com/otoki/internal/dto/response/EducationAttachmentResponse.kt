package com.otoki.internal.dto.response

/**
 * 교육 게시물 첨부파일 Response
 */
data class EducationAttachmentResponse(
    val id: String,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long  // bytes
)
