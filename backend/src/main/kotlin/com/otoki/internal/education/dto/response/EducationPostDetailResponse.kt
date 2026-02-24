package com.otoki.internal.education.dto.response

/**
 * 교육 게시물 상세 Response
 */
data class EducationPostDetailResponse(
    val id: String,
    val category: String,           // edu_code (e.g., "TASTING_MANUAL")
    val categoryName: String,       // edu_code_nm (e.g., "시식 매뉴얼")
    val title: String,
    val content: String,
    val createdAt: String,          // ISO 8601 형식
    // Phase2: EducationImageResponse 주석 처리됨 - Any로 대체
    val images: List<Any> = emptyList(),
    val attachments: List<EducationAttachmentResponse>
)
