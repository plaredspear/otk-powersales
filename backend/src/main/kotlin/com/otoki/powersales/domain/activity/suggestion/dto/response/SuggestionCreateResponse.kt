package com.otoki.powersales.domain.activity.suggestion.dto.response

/**
 * 제안 등록 응답 DTO (Spec #664 P2-B §2.2).
 */
data class SuggestionCreateResponse(
    val id: Long,
    val proposalNumber: String,
    val attachments: List<SuggestionAttachment>
)

data class SuggestionAttachment(
    val id: Long,
    val s3Url: String,
    val fileName: String?,
    val sortOrder: Int
)
