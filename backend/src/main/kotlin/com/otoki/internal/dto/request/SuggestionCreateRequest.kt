package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 제안 등록 요청 DTO
 * multipart/form-data로 전송되며, 사진 파일은 MultipartFile로 별도 처리
 */
data class SuggestionCreateRequest(
    @field:NotBlank(message = "분류는 필수입니다")
    val category: String?,

    val productCode: String? = null,

    @field:NotBlank(message = "제안 제목은 필수입니다")
    @field:Size(max = 200, message = "제안 제목은 최대 200자입니다")
    val title: String?,

    @field:NotBlank(message = "제안 내용은 필수입니다")
    @field:Size(max = 2000, message = "제안 내용은 최대 2000자입니다")
    val content: String?
)
